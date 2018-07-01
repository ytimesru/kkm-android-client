package ru.ytimes.client.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;

import ru.ytimes.client.main.Utils;

/**
 * Created by andrey on 26.06.18.
 */
public class Sam4sBuilder extends com.sam4s.printer.Sam4sBuilder {
    public static final int WIDTH = 42;

    private String ip;
    private int port = 6001;

    public Sam4sBuilder(String ip, int port) throws Exception {
        super("ellix", com.sam4s.printer.Sam4sBuilder.LANG_EN);
        this.ip = ip;
        this.port = port;
    }

    public void addText(String data) throws Exception {
        data += "\n";
        byte mNoData = 0;
        Object mText = null;
        byte[] mText1;
        mText1 = data.getBytes("cp866");

        Field field = com.sam4s.printer.Sam4sBuilder.class.getDeclaredField("mCommandData");
        field.setAccessible(true);

        ByteArrayOutputStream stream = (ByteArrayOutputStream) field.get(this);
        stream.write(mNoData);
        stream.write(mText1);
        stream.write(mNoData);
    }

    public void addPosition(String s1, Double value) throws Exception {
        addPosition(s1, value, '.');
    }

    public void addPosition(String s1, Double value, char ch) throws Exception {
        value = Utils.roundTo(value, 2);
        addText(StringUtils.twoColumn(s1, value + "", WIDTH, ch));
    }

    public void add2Col(String s1, String s2) throws Exception {
        addText(StringUtils.twoColumn(s1, s2, WIDTH));
    }

    public void addDelim() throws Exception {
        addText(StringUtils.generate('-', WIDTH));
    }

    public void sendData() throws IOException, NoSuchFieldException, IllegalAccessException {
        Field field = com.sam4s.printer.Sam4sBuilder.class.getDeclaredField("mCommandData");
        field.setAccessible(true);

        Socket socket = new Socket(ip, port);
        if(!socket.isConnected()) {
            throw new RuntimeException("Not connected");
        }

        OutputStream mOut = socket.getOutputStream();
        InputStream mIn = socket.getInputStream();
        mOut.flush();
        mOut.write(((ByteArrayOutputStream)field.get(this)).toByteArray());

        if(mOut != null) {
            mOut.close();
        }

        if(mIn != null) {
            mIn.close();
        }

        if(socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

}
