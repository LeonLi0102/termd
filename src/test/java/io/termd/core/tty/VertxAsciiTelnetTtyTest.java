package io.termd.core.tty;

import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetServerRule;

import java.io.Closeable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxAsciiTelnetTtyTest extends TelnetTtyTestBase {

  public VertxAsciiTelnetTtyTest() {
    binary = false;
  }

  @Override
  protected Function<Supplier<TelnetHandler>, Closeable> serverFactory() {
    return TelnetServerRule.VERTX_SERVER;
  }
}