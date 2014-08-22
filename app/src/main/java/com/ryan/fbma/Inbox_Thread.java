package com.ryan.fbma;

import java.io.Serializable;

public class Inbox_Thread implements Serializable {

    private final Friend[] names;
    private final long threadID;

    public Inbox_Thread(Friend[] names, long threadID) {
        this.names = names;
        this.threadID = threadID;
    }

    /**
     * @return the names
     */
    public Friend[] getNames() {
        return names;
    }

    /**
     * @return the threadID
     */
    public long getThreadID() {
        return threadID;
    }

    public String[] getNamesString() {
        final String[] temp = new String[names.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = names[i].getName();
        }
        return temp;
    }
}
