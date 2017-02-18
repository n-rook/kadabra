# Known issues

It looks like the bot can't recover from a bad connection. If a request fails once,
the stub to kotlin will consistently fail forevermore.

The organizer doesn't understand U-Turn. As such, if the bot decides to use
U-Turn, it'll deadlock.

Automatic realistic team generation is still dubious.

## TO-DO LIST:

- Bring in Showdown as a submodule
- If force_switch is true, don't pick the active Pokemon, it won't work