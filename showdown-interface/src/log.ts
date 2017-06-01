/**
 * Represents log lines sent or received from the Showdown server.
 *
 * Unrelated to application logging.
 */

/**
 * A log line from the server.
 */
export class ServerLog {
  /** The type of message.
   *
   * For instance, given the log line
   * |switch|p2a: Rotom|Rotom-Wash|303/303
   * The class is 'switch'.
   */
  public readonly class: string;

  /**
   * The remainder of the message.
   *
   * For instance, given the log line
   * |choice||switch 5
   * The content is ['', 'switch 5']
   */
  public readonly content: ReadonlyArray<string>;

  constructor(class_: string, content: string[]) {
    this.class = class_;
    this.content = Object.freeze(content.slice());
  }

  toString(): string {
    return `|${this.class}|${this.content.join('|')}`;
  }

  toLogLine(): ILogLine {
    return {
      received: {
        class: this.class,
        content: this.content
      }
    };
  }
}

/**
 * A message sent to the server.
 */
export class SentMessage {
  public readonly content: string;

  constructor(content: string) {
    this.content = content;
  }

  toString() {
    return this.content;
  }

  toLogLine(): ILogLine {
    return {sent: this.content};
  }
}

/**
 * Interface representing a LogLine proto message.
 */
export class ILogLine {
  sent?: string;
  received?: IReceivedMessage;
}

export class IReceivedMessage {
  class: string;
  content: ReadonlyArray<string>;
}
