package com.casc.rfidscanner.backend;

import android.util.Log;

import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.helper.param.MessageBillReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.NewClientMessage;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    private static String TAG = HttpServer.class.getSimpleName();

    private static String[] ORDER_NUMBER = new String[]{
            "①","②","③","④","⑤","⑥","⑦","⑧","⑨","⑩",
            "⑪","⑫","⑬","⑭","⑮","⑯","⑰","⑱","⑲","⑳"};

    private final Map<String, Client> clientMap = new LinkedHashMap<>();

    public HttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 如果不对每次请求parseBody的话，那么下一次请求时这个残留的body会被合并到一起，真鸡儿尴尬
        if (Method.POST.equals(session.getMethod())) {
            Client client;
            String lineName = session.getParameters().get("line").get(0);
            Map<String, String> body = new HashMap<>();
            try {
                session.parseBody(body);
                synchronized (clientMap) {
                    if (!clientMap.containsKey(session.getRemoteIpAddress())) {
                        client = new Client(lineName, session.getRemoteIpAddress());
                        clientMap.put(client.getIPStr(), client);
                        EventBus.getDefault().post(new NewClientMessage(client));
                        return generateJsonResponse(201, "NewClient");
                    } else {
                        client = clientMap.get(session.getRemoteIpAddress());
                        client.setName(lineName);
                    }
                }
                String data = body.get("postData");
                switch (session.getUri()) {
                    case "/heartbeat":
                        client.update();
                        //client.setDataIncoming(true);
                        break;
                    case "/bill_delivery":
                        client.setDataIncoming(true);
                        EventBus.getDefault().post(new Gson().fromJson(data, MessageBillDelivery.class).setClient(client));
                        break;
                    case "/bill_reflux":
                        client.setDataIncoming(true);
                        EventBus.getDefault().post(new Gson().fromJson(data, MessageBillReflux.class).setClient(client));
                        break;
                    case "/bill_bucket":
                        client.setDataIncoming(true);
                        EventBus.getDefault().post(new Gson().fromJson(data, MessageBillBucket.class).setClient(client));
                        break;
                    case "/bill_complete":
                        EventBus.getDefault().post(new Gson().fromJson(data, MessageBillComplete.class).setClient(client));
                        break;
                }
            } catch (IOException | ResponseException e) {
                e.printStackTrace();
            }
            return generateJsonResponse(200, "ShouDaoLe,ShaBi");
        }
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/html", "ShaBi");
    }

    private Response generateJsonResponse(int code, String message) {
        return newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                new Gson().toJson(new Reply(code, message)));
    }
}
