package com.whitesky.tv.projectorlauncher.service.mqtt;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by jeff on 18-4-9.
 */

public class WakeupGroupThread extends Thread {

    private static final String TAG = WakeupGroupThread.class.getSimpleName();

    private Deque<String> waitToWakeupList = new ArrayDeque<>();

    @Override
    public void run(){
        Log.i(TAG,"thread start!");
        while (!waitToWakeupList.isEmpty()) {
            wakeupDevice(waitToWakeupList.pop().trim());
        }
        Log.i(TAG,"thread done!");
    }

    public void add(String mac) {
        waitToWakeupList.add(mac);
    }

    public void clear() {
        waitToWakeupList.clear();
    }

    public static String wakeupDevice(String mac){
        String ip = "255.255.255.255";
        int port = 2304;
        String magicPackage = "0xFFFFFFFFFFFF";
        for(int i = 0; i < 16; i++){
            magicPackage += mac.replace(":","");
        }
        byte[] command = hexToBinary(magicPackage);
        //      for(Byte b : command){
        //        System.out.print(b.byteValue());
        //      }
        try {
            InetAddress address = InetAddress.getByName(ip);
            MulticastSocket socket = new MulticastSocket(port);
            DatagramPacket packet = new DatagramPacket(command, command.length-1, address, port);
            socket.send(packet);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mac;
    }

    private static byte[] hexToBinary(String hexString){
        byte[] result = new byte[hexString.length()/2];
        hexString = hexString.toUpperCase().replace("0X", "");
        char tmp1 = '0';
        char tmp2 = '0';
        for(int i = 0; i < hexString.length(); i += 2){
            tmp1 = hexString.charAt(i);
            tmp2 = hexString.charAt(i+1);
            result[i/2] = (byte)((hexToDec(tmp1)<<4)|(hexToDec(tmp2)));
        }
        return result;
    }

    private static byte hexToDec(char c){
        int index = "0123456789ABCDEF".indexOf(c);
        return (byte)index;
    }
}
