package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.Helper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventHandler implements Handler<Event> {

  final Map<String, Function> functions = new HashMap<>();
  final Handler<int[]> output;
  final LinkedList<int[]> lines = new LinkedList<>();
  final LineBuffer buffer = new LineBuffer();
  final Handler<RequestContext> handler;

  public EventHandler(Handler<int[]> output, Handler<RequestContext> handler) {
    output.handle(new int[]{'%', ' '});
    this.output = output;
    this.handler = handler;
  }

  public EventHandler addFunction(Function function) {
    functions.put(function.getName(), function);
    return this;
  }

  private boolean handling;
  private LinkedList<Integer> escaped = new LinkedList<>();
  private int status = 0;
  private EscapeFilter filter = new EscapeFilter(new Escaper() {
    @Override
    public void escaping() {
      status = 1;
    }
    @Override
    public void escaped(int ch) {
      if (ch != '\r') {
        escaped.add((int) '\\');
        escaped.add(ch);
      }
      status = 0;
    }
    @Override
    public void beginQuotes(int delim) {
      escaped.add(delim);
      status = 2;
    }
    @Override
    public void endQuotes(int delim) {
      escaped.add(delim);
      status = 0;
    }
    @Override
    public void handle(Integer event) {
      escaped.add(event);
    }
  });

  public void handle(Event event) {
    handle(event, null);
  }

  public void handle(Event event, final Handler<Void> doneHandler) {
    if (handling) {
      throw new IllegalStateException();
    }
    handling = true;
    LineBuffer copy = new LineBuffer(buffer);
    if (event instanceof KeyEvent) {
      KeyEvent key = (KeyEvent) event;
      if (key.length() == 1 && key.getAt(0) == '\r') {
        for (int j : buffer) {
          filter.handle(j);
        }
        if (status == 1) {
          filter.handle((int) '\r'); // Correct status
          output.handle(new int[]{'\r', '\n', '>', ' '});
          buffer.setSize(0);
          copy.setSize(0);
        } else {
          int[] l = new int[this.escaped.size()];
          for (int index = 0;index < l.length;index++) {
            l[index] = this.escaped.get(index);
          }
          escaped.clear();
          lines.add(l);
          if (status == 2) {
            output.handle(new int[]{'\r', '\n', '>', ' '});
            buffer.setSize(0);
            copy.setSize(0);
          } else {
            final StringBuilder raw = new StringBuilder();
            for (int index = 0;index < lines.size();index++) {
              int[] a = lines.get(index);
              if (index > 0) {
                raw.append('\n'); // Use \n for processing
              }
              for (int b : a) {
                raw.appendCodePoint(b);
              }
            }
            lines.clear();
            escaped.clear();
            output.handle(new int[]{'\r', '\n'});
            buffer.setSize(0);
            handler.handle(new RequestContext() {

              @Override
              public String getRaw() {
                return raw.toString();
              }

              @Override
              public RequestContext write(String s) {
                output.handle(Helper.toCodePoints(s));
                return this;
              }

              @Override
              public void end() {
                output.handle(new int[]{'%', ' '});
                handling = false;
                if (doneHandler != null) {
                  doneHandler.handle(null);
                }
              }
            });
            return;
          }
        }
      } else {
        for (int i = 0;i < key.length();i++) {
          int codePoint = key.getAt(i);
          buffer.insert(codePoint);
        }
      }
    } else {
      FunctionEvent fname = (FunctionEvent) event;
      Function function = functions.get(fname.getName());
      if (function != null) {
        function.call(buffer);
      } else {
        System.out.println("Unimplemented function " + fname.getName());
      }
    }
    LinkedList<Integer> a = copy.compute(buffer);
    int[] t = new int[a.size()];
    for (int index = 0;index < a.size();index++) {
      t[index] = a.get(index);
    }
    output.handle(t);
    handling = false;
    if (doneHandler != null) {
      doneHandler.handle(null);
    }
  }
}
