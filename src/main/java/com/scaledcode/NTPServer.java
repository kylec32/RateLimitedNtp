package com.scaledcode;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NTPServer {

    private static final int NTP_PORT = 123;
    private static final byte MODE_SERVER = 4;
    private static final byte STRATUM_KOD = 0;
    private static final byte[] KOD_CODE = "RATE".getBytes();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(NTP_PORT)) {
            System.out.println("NTP Server is running and listening on port " + NTP_PORT);

            while (true) {
                byte[] buffer = new byte[48];
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);

                InetAddress clientAddress = requestPacket.getAddress();
                int clientPort = requestPacket.getPort();

                long transmitTimestamp = extractTransmitTimestamp(buffer);

                DatagramPacket responsePacket = createKoDPacket(clientAddress, clientPort, transmitTimestamp);
                socket.send(responsePacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long extractTransmitTimestamp(byte[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        // The transmit timestamp starts at byte 40
        long seconds = byteBuffer.getInt(40) & 0xFFFFFFFFL;
        long fraction = byteBuffer.getInt(44) & 0xFFFFFFFFL;
        return (seconds << 32) | fraction;
    }

    private static DatagramPacket createKoDPacket(InetAddress clientAddress, int clientPort, long offset) {
        byte[] response = new byte[48];

        // Set the leap indicator, version number, and mode
        response[0] = (byte) (0b11000000 | (4 << 3) | MODE_SERVER);

        // Set the stratum to 0 (kiss-of-death)
        response[1] = STRATUM_KOD;

        // Set the reference identifier to the KoD code (RATE)
        System.arraycopy(KOD_CODE, 0, response, 12, KOD_CODE.length);

        ByteBuffer byteBuffer = ByteBuffer.wrap(response);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(24, (int) (offset >> 32)); // seconds
        byteBuffer.putInt(28, (int) (offset & 0xFFFFFFFFL)); // fraction

        return new DatagramPacket(response, response.length, clientAddress, clientPort);
    }
}

