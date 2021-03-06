package com.taobao.top.link.schedule;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.taobao.top.link.DefaultLoggerFactory;
import com.taobao.top.link.LinkException;
import com.taobao.top.link.LoggerFactory;
import com.taobao.top.link.Text;

public class SchedulerTest {
	private LoggerFactory loggerFactory = new DefaultLoggerFactory(true, true, true, true, true);

	@Test
	public void queue_test() {
		Queue<String> queue = new ConcurrentLinkedQueue<String>();
		queue.add("123");
		assertNotNull(queue.peek());
		assertNotNull(queue.poll());
	}

	@Test
	public void semaphore_test() throws InterruptedException {
		final Semaphore semaphore = new Semaphore(0);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				semaphore.release();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				semaphore.release();
				semaphore.release();
			}
		}).start();
		semaphore.acquire();
		semaphore.acquire(2);
	}

	@Test
	public void dispatch_test() throws LinkException, InterruptedException {
		Scheduler<String> scheduler = new Scheduler<String>(loggerFactory);
		final CountDownLatch latch = new CountDownLatch(10);
		for (int i = 0; i < 10; i++)
			scheduler.schedule("user-" + i, new Runnable() {
				@Override
				public void run() {
					latch.countDown();
				}
			});
		scheduler.dispatch();
		latch.await();
	}

	@Test
	public void dispatch_reject_test() throws LinkException, InterruptedException {
		final Queue<Runnable> queue = new ArrayBlockingQueue<Runnable>(2, false);
		Scheduler<String> scheduler = new Scheduler<String>(loggerFactory) {
			@Override
			protected Queue<Runnable> createTaskQueue(String t) {
				assertEquals("user", t);
				return queue;
			}
		};
		scheduler.setThreadPool(new ThreadPoolExecutor(1, 1, 300, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()) {
			@Override
			public void execute(Runnable command) {
				throw new RejectedExecutionException();
			}
		});
		scheduler.schedule("user", new Runnable() {
			@Override
			public void run() {
			}
		});
		scheduler.dispatch();
		assertEquals(1, queue.size());

		scheduler.setThreadPool(Executors.newFixedThreadPool(1));
		scheduler.dispatch();
		assertEquals(0, queue.size());
	}

	@Test
	public void start_stop_test() throws InterruptedException, LinkException {
		final CountDownLatch latch = new CountDownLatch(2);
		Scheduler<String> scheduler = new Scheduler<String>(loggerFactory);
		scheduler.start();
		scheduler.schedule("user", new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		Thread.sleep(500);
		scheduler.stop();
		scheduler.start();
		scheduler.schedule("user", new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		latch.await();
		scheduler.stop();
	}

	@Test
	public void checker_test() throws LinkException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Scheduler<String> scheduler = new Scheduler<String>(loggerFactory);
		scheduler.start();
		scheduler.prepareChecker(1, 100);
		scheduler.disposeDispatcher();
		scheduler.running = true;
		scheduler.schedule("user", new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		latch.await();
		scheduler.stop();
	}

	@Test
	public void schedule_test() throws InterruptedException, LinkException {
		Scheduler<String> scheduler = new Scheduler<String>();
		scheduler.start();
		final CountDownLatch latch = new CountDownLatch(1);
		scheduler.schedule("user", new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		latch.await();
		scheduler.stop();
	}

	@Test
	public void drop_test() throws InterruptedException, LinkException {
		final Scheduler<String> scheduler = new Scheduler<String>();
		scheduler.setUserMaxPendingCount(10000);
		scheduler.start();
		final AtomicBoolean flag = new AtomicBoolean(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (flag.get()) {
					try {
						scheduler.schedule("user", new Runnable() {
							@Override
							public void run() {
							}
						});
					} catch (LinkException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		scheduler.drop("user");
		scheduler.stop();
		flag.set(false);
	}

	@Test(expected = LinkException.class)
	public void got_max_test() throws InterruptedException, LinkException {
		Scheduler<String> scheduler = new Scheduler<String>();
		scheduler.setUserMaxPendingCount(10);
		scheduler.start();
		for (int i = 0; i < 100; i++) {
			try {
				scheduler.schedule("user", new Runnable() {
					@Override
					public void run() {
					}
				});
			} catch (LinkException e) {
				scheduler.stop();
				assertEquals(String.format(Text.SCHEDULE_GOT_MAX, 10), e.getMessage());
				throw e;
			}
		}
	}

	@Test
	public void schedule_sequence_test() throws InterruptedException, LinkException {
		final Scheduler<String> scheduler = new Scheduler<String>(loggerFactory);
		scheduler.setUserMaxPendingCount(10000);
		scheduler.start();
		int count = 10000;
		final CountDownLatch latch = new CountDownLatch(count);
		long begin = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			scheduler.schedule("user", new Runnable() {
				@Override
				public void run() {
					latch.countDown();
				}
			});

		}
		latch.await();
		System.out.println(count + " cost=" + (System.currentTimeMillis() - begin));
		scheduler.stop();
		// 1000 cost=36
	}

	@Test
	public void schedule_threaded_test() throws InterruptedException, LinkException {
		final Scheduler<String> scheduler = new Scheduler<String>(loggerFactory);
		scheduler.setUserMaxPendingCount(1000);
		scheduler.start();
		final int count = 10000;
		int thread = 4;
		final CountDownLatch latch = new CountDownLatch(count * thread);
		long begin = System.currentTimeMillis();
		for (int i = 0; i < thread; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < count; i++) {
						try {
							scheduler.schedule("user" + i, new Runnable() {
								@Override
								public void run() {
									latch.countDown();
								}
							});
						} catch (LinkException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		}
		latch.await();
		System.out.println(count * thread + " cost=" + (System.currentTimeMillis() - begin));
		scheduler.stop();
		// 1000 cost=182
	}

	@Test
	public void schedule_by_type_and_batch_test() throws LinkException, InterruptedException {
		BatchedScheduler<String> scheduler = new BatchedScheduler<String>(loggerFactory) {
			@Override
			protected boolean enableBatch(Runnable task) {
				return task instanceof Task1;
			}

			@Override
			protected boolean areInSameBatch(Runnable next, Runnable first) {
				return next != null &&
						first instanceof Task1 &&
						next instanceof Task1;
			}
		};
		scheduler.setUserMaxPendingCount(10000);

		CountDownLatch latch = new CountDownLatch(3);
		AtomicInteger counter = new AtomicInteger();
		AtomicLong threadId = new AtomicLong();

		scheduler.schedule("user", new Task1(latch, counter, 0, threadId));
		scheduler.schedule("user", new Task1(latch, counter, 1, threadId));
		scheduler.schedule("user", new Task1(latch, counter, 2, threadId));
		scheduler.start();
		latch.await();
		scheduler.stop();

		latch = new CountDownLatch(4);
		counter = new AtomicInteger();
		threadId = new AtomicLong();
		scheduler.schedule("user", new Task2(latch));
		scheduler.schedule("user", new Task1(latch, counter, 0, threadId));
		scheduler.schedule("user", new Task1(latch, counter, 1, threadId));
		scheduler.schedule("user", new Task2(latch));
		// only 1 thread that batched task should be dispath in next time, would be dropped
		scheduler.setThreadPool(new ThreadPoolExecutor(1, 1, 300, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()));
		scheduler.start();
		latch.await();
		scheduler.stop();
	}

	class Task1 implements Runnable {
		CountDownLatch latch;
		AtomicInteger counter;
		int expected;
		AtomicLong threadId;

		public Task1(CountDownLatch latch, AtomicInteger counter, int expected, AtomicLong threadId) {
			this.latch = latch;
			this.counter = counter;
			this.expected = expected;
			this.threadId = threadId;
		}

		@Override
		public void run() {
			if (this.threadId.get() <= 0)
				this.threadId.set(Thread.currentThread().getId());
			// sequence and in one thread
			assertEquals(this.expected, this.counter.getAndIncrement());
			assertEquals(this.threadId.get(), Thread.currentThread().getId());
			this.latch.countDown();
			System.out.println("task1 executed. expected=" + this.expected + " in thread#" + Thread.currentThread().getId());
		}
	}

	class Task2 implements Runnable {
		CountDownLatch latch;

		public Task2(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void run() {
			this.latch.countDown();
			System.out.println("task2 executed in thread#" + Thread.currentThread().getId());
		}
	}
}