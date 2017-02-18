# Known issues

It looks like the bot can't recover from a bad connection. If a request fails once,
the stub to kotlin will consistently fail forevermore.

The organizer doesn't understand U-Turn. As such, if the bot decides to use
U-Turn, it'll deadlock.

Automatic realistic team generation is still dubious.

## TO-DO LIST:

- Try PromisifyAll in ai_client
- Rename ai_client to battle_client
- Bring in Showdown as a submodule