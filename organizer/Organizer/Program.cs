using Microsoft.Extensions.Logging;
using System;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

/**
 * This server organizes matches by serving as an interface between the
 * Pokemon Showdown server and AI clients.
 * 
 * The design is as follows:
 * 
 * Pokemon Showdown uses WebSockets to communicate with clients. This makes sense, but it's a bit
 * awkward for AI bots, which don't need the full breadth of capabilities Showdown provides.
 * These clients would prefer just to be asked specific questions through HTTP calls. As such,
 * Organizer maintains a WebSockets connection to the Showdown server and asks questions of the
 * client.
 */
namespace Organizer
{

    class Program
    {
        private static ILogger logger = Logging.Logger<Program>();

        static void Main(string[] args)
        {
            try
            {
                Task work = RunLoop();
                work.Wait();
            } catch (Exception e)
            {
                logger.LogError(e.ToString());
            }
        }

        async static Task RunLoop()
        {
            ShowdownConnection connection = await ShowdownConnection.Connect();

            await connection.ReceiveAsync();
            await connection.ReceiveAsync();
            await connection.SendAsync("Hello");

            while (true)
            {
                String message = await connection.ReceiveAsync();
                logger.LogInformation("Received a message!\n" + message + "\n");
            }
        }


        async static Task connect()
        {
            logger.LogInformation("hello");
            Uri address = new Uri("ws://localhost:8000/showdown/000/aaaaaaaa/websocket");
            ClientWebSocket connection = new ClientWebSocket();
            CancellationToken token = new CancellationToken();
            await connection.ConnectAsync(address, token);
           
            Console.WriteLine("Hello 2");

            CancellationToken sendToken = new CancellationToken();
            byte[] messageArray = Encoding.ASCII.GetBytes("test");
            ArraySegment<byte> message = new ArraySegment<byte>(messageArray);
            await connection.SendAsync(message, WebSocketMessageType.Text, true, sendToken);
            Console.WriteLine("Hello 3");
        }
    }
}
