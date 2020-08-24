package me.cynadyde.simplemachines.util;

public class PacketUtils {

    private PacketUtils() {}

    // from https://wiki.vg/Protocol#Block_Break_Animation

    public static int readVarInt() {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static void writeVarInt(int value) {
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            writeByte(temp);
        } while (value != 0);
    }

    public static void writePosition() {
        ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
    }

    public static int readPosition() {
        val = read_unsigned_long();
        x = val >> 38;
        y = val & 0xFFF;
        z = (val << 26 >> 38);
    }

    // Block Break Animation Packet (Play/ClientBound)
    // 0x09 - packet id
    // VarInt - animation id
    // Position - animation pos
    // Byte - animation stage 0-9

}
