package com.reactlibrary;

import android.os.AsyncTask;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.sun.mail.imap.IMAPFolder;

import java.util.Properties;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.*;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

public class RNImapMailerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private String mailhost;
    private String port;
    private Boolean ssl;
    private String username;
    private String password;
    private IMAPFolder folder = null;
    private Store store = null;

    public RNImapMailerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }


    @ReactMethod
    void connect(final ReadableMap obj, final Promise promise) {
        mailhost = obj.getString("mailhost");
        port = obj.getString("port");
        ssl = obj.getBoolean("ssl");
        username = obj.getString("username");
        password = obj.getString("password");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Properties props = System.getProperties();
                    props.setProperty("mail.store.protocol", "imaps");

                    Session session = Session.getDefaultInstance(props, null);

                    store = session.getStore("imaps");
                    store.connect(mailhost, username, password);

                    // folder = (IMAPFolder) store.getFolder("[Gmail]/Spam"); // This doesn't work for other email account
                    folder = (IMAPFolder) store.getFolder("inbox"); // This works for both email account


                    if (!folder.isOpen())
                        folder.open(Folder.READ_WRITE);

                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                    promise.reject(e.getMessage());
                } catch (MessagingException e) {
                    e.printStackTrace();
                    promise.reject(e.getMessage());
                }

            }
        });
    }

    @ReactMethod
    void close(final Promise promise) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (folder != null && folder.isOpen()) {
                        folder.close(true);

                    }
                    if (store != null) {
                        store.close();

                    }

                } catch (MessagingException e) {
                    e.printStackTrace();
                    promise.reject(new Error("Unable to close connection"));
                } finally {
                    promise.resolve(null);
                }
            }
        });
    }

    @ReactMethod
    void checkMail(final Promise promise) {
        AsyncTask.execute(new Runnable() {

            String subject = null;
            Flags.Flag flag = null;


            @Override
            public void run() {
                WritableNativeMap obj = new WritableNativeMap();
                WritableNativeArray msgs = new WritableNativeArray();
                try {

                    Message[] messages = folder.getMessages();
                    obj.putInt("noOfMessages", folder.getMessageCount());
                    obj.putInt("noOfUnreadMessages", folder.getUnreadMessageCount());

                    System.out.println("No of Messages : " + folder.getMessageCount());
                    System.out.println("No of Unread Messages : " + folder.getUnreadMessageCount());
                    System.out.println(messages.length);

                    for (int i = 0; i < messages.length && i < 20; i++) {

                        WritableNativeMap tmp = new WritableNativeMap();

                        System.out.println("*****************************************************************************");
                        System.out.println("MESSAGE " + (i + 1) + ":");
                        Message msg = messages[i];
                        //System.out.println(msg.getMessageNumber());
                        //Object String;
                        //System.out.println(folder.getUID(msg)

                        subject = msg.getSubject();

                        tmp.putString("subject", subject);
                        tmp.putString("from", msg.getFrom()[0].toString());
                        tmp.putString("to", msg.getAllRecipients()[0].toString());
                        tmp.putString("date", msg.getReceivedDate().toString());
                        tmp.putInt("size", msg.getSize());
                        tmp.putString("body", msg.getContent().toString());
                        tmp.putString("bodyType", msg.getContentType());
                        msgs.pushMap(tmp);


                        System.out.println("Subject: " + subject);
                        System.out.println("From: " + msg.getFrom()[0]);
                        System.out.println("To: " + msg.getAllRecipients()[0]);
                        System.out.println("Date: " + msg.getReceivedDate());
                        System.out.println("Size: " + msg.getSize());
                        System.out.println(msg.getFlags());
                        System.out.println("Body: \n" + msg.getContent());
                        System.out.println(msg.getContentType());

                    }
                    obj.putArray("messages", msgs);
                } catch (Exception e) {
                    e.printStackTrace();
                    promise.reject(e.getMessage());
                } finally {
                    promise.resolve(obj);
                }
            }
        });

    }

    @Override
    public String getName() {
        return "RNImapMailer";
    }
}
