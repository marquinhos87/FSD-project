package fsd;

import java.nio.channels.CompletionHandler;

public class Box<T> {
    /*T content;
    CompletionHandler<String,Object> ch

    public void put(T s) {
        content = s;
        ch.completed(s, null);
    }

    public void get() {
        while(content == null)
            wait();
    }

    public void get(CompletionHandler<String,Object> ch) {
        if (content == null)
            this.ch = ch;
        else
            ch.completed(content, null);
    }
    */
}
