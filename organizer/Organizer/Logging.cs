using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Organizer
{
    static class Logging
    {
        private static ILoggerFactory loggerFactory =
            new LoggerFactory().AddDebug();

        public static ILogger Logger<T>() => loggerFactory.CreateLogger<T>();
    }
}
