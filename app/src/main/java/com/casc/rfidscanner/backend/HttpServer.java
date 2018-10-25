package com.casc.rfidscanner.backend;

import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    private static String TAG = HttpServer.class.getSimpleName();

    private static String[] ORDER_NUMBER = new String[]{
            "①","②","③","④","⑤","⑥","⑦","⑧","⑨","⑩",
            "⑪","⑫","⑬","⑭","⑮","⑯","⑰","⑱","⑲","⑳"};

    public HttpServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 如果不对每次请求parseBody的话，那么下一次请求时这个残留的body会被合并到一起，真鸡儿尴尬
        // Log.i(TAG,  session.getRemoteIpAddress() + ":" + session.getMethod().toString() + ":" + session.getUri());
        if (Method.POST.equals(session.getMethod())) {
            String lineName = session.getParameters().get("line").get(0);
            Map<String, String> body = new HashMap<>();
            try {
                session.parseBody(body);
                String data = body.get("postData");
                switch (session.getUri()) {
                    case "/heartbeat":
                        //client.setDataIncoming(true);
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
                CommonUtils.toJson(new Reply(code, message)));
    }
}
