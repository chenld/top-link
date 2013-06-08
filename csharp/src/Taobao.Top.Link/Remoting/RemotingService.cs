﻿using System;
using System.Collections.Generic;
using System.Text;
using Taobao.Top.Link.Channel;
using Taobao.Top.Link.Remoting.Serialization.Json;

namespace Taobao.Top.Link.Remoting
{
    /// <summary>remoting helper for easy using
    /// </summary>
    public static class RemotingService
    {
        private static RemotingHandler handler;
        static RemotingService()
        {
            ILoggerFactory loggerFactory = DefaultLoggerFactory.Default;
            handler = new RemotingHandler(loggerFactory
                , new ClientChannelSharedSelector(loggerFactory)
                , new CrossLanguageSerializationFactory());
        }

        /// <summary>create remote service object
        /// </summary>
        /// <typeparam name="T">service type</typeparam>
        /// <param name="remoteUri">remote service address</param>
        /// <returns></returns>
        public static T Connect<T>(string remoteUri) where T : class
        {
            return new DynamicProxy<T>(new Uri(remoteUri), handler).GetProxy();
        }
        /// <summary>create remote service object
        /// </summary>
        /// <param name="classToProxy">service type for proxy</param>
        /// <param name="remoteUri">remote service address</param>
        /// <returns></returns>
        public static object Connect(Type classToProxy, string remoteUri)
        {
            return new DynamicProxy(classToProxy, new Uri(remoteUri), handler).GetTransparentProxy();
        }
    }
}