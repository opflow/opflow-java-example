package com.devebot.opflow.example;

import com.devebot.opflow.OpflowBuilder;
import com.devebot.opflow.OpflowMessage;
import com.devebot.opflow.OpflowRpcMaster;
import com.devebot.opflow.OpflowRpcRequest;
import com.devebot.opflow.OpflowRpcResult;
import com.devebot.opflow.OpflowUtil;
import com.devebot.opflow.exception.OpflowBootstrapException;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FibonacciRpcMaster {

    private final OpflowRpcMaster master;
    
    public FibonacciRpcMaster() throws OpflowBootstrapException {
        master = OpflowBuilder.createRpcMaster();
    }
    
    public OpflowRpcRequest request(final int number) {
        return request(number, 180000, false);
    }
    
    public OpflowRpcRequest request(final int number, final long timeout) {
        return request(number, timeout, false);
    }
    
    public OpflowRpcRequest request(final int number, final boolean forked) {
        return request(number, 180000, forked);
    }
    
    public OpflowRpcRequest request(final int number, final long timeout, final boolean forked) {
        System.out.println("[+] Request[" + number + "] starting");
        OpflowRpcRequest req = master.request("fibonacci",
                OpflowUtil.buildMap().put("number", number).toString(),
                OpflowUtil.buildMap(new OpflowUtil.MapListener() {
                    @Override
                    public void transform(Map<String, Object> opts) {
                        if (timeout > 0) opts.put("timeout", timeout);
                        if (forked) opts.put("mode", "forked");
                    }
                }).toMap());
        System.out.println("[x] Request[" + number + "] finished");
        return req;
    }
    
    public Queue<FibonacciData.Pair> random(final int total, final int left, final int right) {
        final Queue<FibonacciData.Pair> sessions = new ConcurrentLinkedQueue<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i=0; i<total; i++) {
                    int number = random(left, right);
                    sessions.add(new FibonacciData.Pair(i, number, request(number)));
                }
            }
        }).start();
        return sessions;
    }
    
    public String checkState() {
        return master.check().getConnectionState() == 1 ? "opened" : "closed";
    }
    
    public void close() {
        master.close();
    }
    
    public static void main(String[] argv) throws Exception {
        final FibonacciRpcMaster rpc = new FibonacciRpcMaster();
        
        System.out.println("[+] ExampleMaster request");

        OpflowRpcRequest req1 = rpc.request(20, true);
        OpflowRpcRequest req2 = rpc.request(30, true);
        
        OpflowRpcRequest[] reqs = new OpflowRpcRequest[10];
        for(int i = 0; i<reqs.length; i++) {
            reqs[i] = rpc.request(random(20, 40));
        }
        
        // OpflowRpcRequest object works as an Interator<OpflowMessage>
        while(req1.hasNext()) {
            OpflowMessage msg = req1.next();
            if (msg.getInfo().get("workerTag") == null) {
                System.out.println("[-] message1 received: " + msg.getBodyAsString());
            } else {
                System.out.println("[-] message1 received: " + msg.getBodyAsString() + 
                    " / workerTag: " + msg.getInfo().get("workerTag"));
            }
        }
        System.out.println();
        
        // Transforms OpflowRpcRequest object to OpflowRpcResult object
        OpflowRpcResult result2 = req2.extractResult();
        for(OpflowRpcResult.Step step: result2.getProgress()) {
            System.out.println("[-] message2 percent: " + step.getPercent());
        }
        System.out.println("[-] message2 result: " + result2.getValueAsString());
        System.out.println();
        
        for(int i = 0; i<reqs.length; i++) {
            OpflowRpcResult rsts = reqs[i].extractResult();
        }
        System.out.println();
        
        OpflowRpcRequest reqx = rpc.request(50);
        
        while(reqx.hasNext()) {
            OpflowMessage msg = reqx.next();
            if (msg.getInfo().get("workerTag") == null) {
                System.out.println("[-] messagex received: " + msg.getBodyAsString());
            } else {
                System.out.println("[-] messagex received: " + msg.getBodyAsString() + 
                    " / workerTag: " + msg.getInfo().get("workerTag"));
            }
        }
        
        rpc.close();
        
        System.out.println("[-] ExampleMaster has finished");
    }
    
    private static final Random RANDOM = new Random();
    
    private static int random(int min, int max) {
        return RANDOM.nextInt(max + 1 - min) + min;
    }
}
