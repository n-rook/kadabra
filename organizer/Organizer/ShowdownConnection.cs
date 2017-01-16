using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Organizer
{
    class ShowdownConnection
    {
        private static ILogger logger = Logging.Logger<ShowdownConnection>();

        private ClientWebSocket connection;

        private ShowdownConnection(ClientWebSocket connection)
        {
            this.connection = connection;
        }

        public async static Task<ShowdownConnection> Connect()
        {
            logger.LogInformation("Connecting:");
            Uri address = new Uri("ws://localhost:8000/showdown/000/aaaaaaaa/websocket");
            ClientWebSocket connection = new ClientWebSocket();
            CancellationToken token = new CancellationToken();
            await connection.ConnectAsync(address, token);
            return new ShowdownConnection(connection);
        }

        private void confirmStillOpen()
        {
            if (connection.CloseStatus != null)
            {
                throw new Exception("Connection is closed: " + connection.CloseStatusDescription);
            }

        }

        public async Task SendAsync(String message)
        {
            confirmStillOpen();

            logger.LogInformation("Sending message: " + message);
            byte[] buffer = Encoding.UTF8.GetBytes(message);
            await connection.SendAsync(new ArraySegment<byte>(buffer), WebSocketMessageType.Text,
                true, new CancellationToken());
            if (connection.CloseStatus != null)
            {
                throw new Exception("Error sending message:\n" + connection.CloseStatusDescription);
            }
        }

        public async Task<string> ReceiveAsync()
        {
            confirmStillOpen();
            byte[] buffer = new byte[15000];
            await connection.ReceiveAsync(new ArraySegment<byte>(buffer), new CancellationToken());
            return Encoding.UTF8.GetString(buffer);
        }
    }
}
