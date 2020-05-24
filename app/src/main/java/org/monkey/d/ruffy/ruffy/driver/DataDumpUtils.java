package org.monkey.d.ruffy.ruffy.driver;

import android.os.Environment;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class DataDumpUtils {
    static private final File externalStoragePath = new File(Environment.getExternalStorageDirectory().toString());
    static private final File dataDumpFile = new File(externalStoragePath, "RuffyBTBinaryDataDump");
    private static DataOutputStream dataDumpOutStream = null;

    private static void openDataDumpOutStream()
    {
        if (dataDumpOutStream != null)
            return;

        String path = null;
        try {
            path = dataDumpFile.getCanonicalPath();
        } catch (IOException e) {
            Log.e("RUFFY_LOG", "Could not get path to data dump file: " + e + " - no data dump file will be generated!");
            return;
        }

        try {
            dataDumpOutStream = new DataOutputStream(new FileOutputStream(dataDumpFile, true));
            Log.i("RUFFY_LOG","Successfully opened data dump file \"" + path + "\"");
        } catch (FileNotFoundException e) {
            Log.e("RUFFY_LOG", "Could not open data dump file \"" + path + "\": " + e + " - no data dump file will be generated!");
        }
    }

    public static synchronized void writeFrameData(boolean isOutgoingFrameData, byte[] frameData, int frameDataLength)
    {
        if (dataDumpOutStream == null) {
            Log.v("RUFFY_LOG", "Data dump file is not open - opening it");
            openDataDumpOutStream();

            if (dataDumpOutStream == null) {
                Log.e("RUFFY_LOG", "Could not open data dump file; not writing frame data");
                return;
            }
        }

        byte[] lengthPrefix = new byte[4];
        lengthPrefix[0] = (byte)((frameDataLength >> 0) & 0xFF);
        lengthPrefix[1] = (byte)((frameDataLength >> 8) & 0xFF);
        lengthPrefix[2] = (byte)((frameDataLength >> 16) & 0xFF);
        lengthPrefix[3] = (byte)((frameDataLength >> 24) & 0x7F);

        lengthPrefix[3] |= (byte)(isOutgoingFrameData ? 0x80 : 0x00);

        try {
            dataDumpOutStream.write(lengthPrefix, 0, 4);
            dataDumpOutStream.write(frameData, 0, frameDataLength);
            dataDumpOutStream.flush();
            Log.v("RUFFY_LOG","Written " + frameDataLength + " frame data byte(s) to dump data file");
        } catch (Exception e) {
            Log.e("RUFFY_LOG","Could not write frame data to dump data file: " + e);
        }
    }

    public static void logApplicationLayerPacket(ByteBuffer packet, String identifier)
    {
        int versions = packet.get(0);
        int serviceID = packet.get(1);
        int cmdID = ((int)(packet.get(3)) << 8) | (int)(packet.get(2));

        String logLine = "==== APPLICATION_LAYER_PACKET ==== [processAppResponse] ====";
        logLine += "  version " + ((versions >> 4) & 0xF) + "/" + ((versions >> 0) & 0xF);
        logLine += "  serviceID " + serviceID;
        logLine += "  cmdID " + cmdID;
        logLine += "  payloadLength " + (packet.array().length - 4);
        logLine += "  payload " + Utils.byteArrayToHexStringWithOffset(packet.array(), packet.array().length, 4);

        Log.v("RUFFY_LOG", logLine);
    }

    public static void logTransportLayerPacket(List<Byte> packet, String identifier)
    {
        /*
        1. 4 bits    : Packet major version (always set to 0x01)
        2. 4 bits    : Packet minor version (always set to 0x00)
        3. 1 bit     : Sequence bit
        4. 1 bit     : Unused (referred to as "Res1")
        5. 1 bit     : Data reliability bit
        6. 5 bits    : Command ID
        7. 16 bits   : Payload length (in bytes), stored as a 16-bit little endian integer
        8. 4 bits    : Source address (meaning unknown)
        9. 4 bits    : Destination address (meaning unknown)
        10. 13 bytes  : Nonce
        11. n bytes   : Payload
        12. 8 bytes   : Message authentication code
        */

        int versions = packet.get(0);
        int seqRelCmd = packet.get(1);
        int payloadLength = packet.get(2) | (packet.get(3) << 8);
        int addresses = packet.get(4);

        byte[] nonce = new byte[13];
        byte[] payload = new byte[payloadLength];
        byte[] mac = new byte[8];

        for (int i = 0; i < 13; ++i)
            nonce[i] = packet.get(5 + i);
        for (int i = 0; i < payloadLength; ++i)
            payload[i] = packet.get(5 + 13 + i);
        for (int i = 0; i < 8; ++i)
            mac[i] = packet.get(5 + 13 + payloadLength + i);

        String logLine = "==== TRANSPORT_LAYER_PACKET ==== [" + identifier + "] ====";
        logLine += "  version " + ((versions >> 4) & 0xF) + "/" + ((versions >> 0) & 0xF);
        logLine += "  seqbit " + (((seqRelCmd & 0x80) != 0) ? 1 : 0);
        logLine += "  relbit " + (((seqRelCmd & 0x20) != 0) ? 1 : 0);
        logLine += "  cmd " + (seqRelCmd & 0x1F);
        logLine += "  payloadLength " + payloadLength;
        logLine += "  addresses " + ((addresses >> 4) & 0xF) + "/" + ((addresses >> 0) & 0xF);
        logLine += "  nonce " + Utils.byteArrayToHexString(nonce, nonce.length);
        logLine += "  payload " + Utils.byteArrayToHexString(payload, payload.length);
        logLine += "  mac " + Utils.byteArrayToHexString(mac, mac.length);

        Log.v("RUFFY_LOG", logLine);
    }
}
