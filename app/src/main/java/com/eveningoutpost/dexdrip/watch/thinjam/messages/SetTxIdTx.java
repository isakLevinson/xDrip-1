package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;

import java.io.UnsupportedEncodingException;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SET_TXID;

// jamorham

public class SetTxIdTx extends BaseTx {

    private static final int SET_BIT_MMOL = 0;
    private static final int SET_BIT_24_HOUR_CLOCK = 1;
    private static final int SET_BIT_RUN_COLLECTOR = 2;
    private static final int SET_BIT_TREND_FROM_DELTA = 3;
    private static final int SET_BIT_FAILSAFE_TIMING = 4;

    public SetTxIdTx(final String txId, final String mac) {

        // TODO needs parameter checking for length etc
        try {

            init(OPCODE_SET_TXID, 19);
            if (txId == null || txId.length() < 6) {
                // if we have no txid then write zeros
                data.put(JoH.tolerantHexStringToByteArray("00 00 00 00 00 00"), 0, 6);
            } else {
                byte[] txIdBytes = txId.getBytes("UTF-8");
                data.put(txIdBytes, 0, 6);
            }

            byte[] macBytes = JoH.tolerantHexStringToByteArray(mac);
            if (macBytes == null || macBytes.length < 6) {
                // if mac bytes invalid then write zeros
                data.put(JoH.tolerantHexStringToByteArray("00 00 00 00 00 00"), 0, 6);
            } else {
                data.put(reverseBytes(macBytes), 0, 6);

            }

            final double lowAlert = AlertType.getFirstActiveAlertThreshold(false);
            final double highAlert = AlertType.getFirstActiveAlertThreshold(true);

            if (BlueJay.localAlarmsEnabled()) {
                // if high alert is below low or low is below high then it should alarm constantly anyway so we probably don't need to check that being wrong
                // we use defaults if no active high / low alert is present
                data.putShort(lowAlert > 0 ? (short) lowAlert : (short) 71); // low alert mgdl
                data.putShort(highAlert > 0 ? (short) highAlert : (short) 216); // high alert mgdl
            } else {
                data.putShort((short) 0);
                data.putShort((short) 0);
            }
            data.putShort(constructBitfield());
            data.put(screenWakeByte());
        } catch (UnsupportedEncodingException e) {
            // broken
        }
    }


    private short constructBitfield() {
        short bits = 0;
        bits |= (Unitized.usingMgDl() ? 0 : 1); // simplified shift of SET_BIT_MMOL
        bits |= (Pref.getBooleanDefaultFalse("bluejay_option_24_hour_clock") ? 1 : 0) << SET_BIT_24_HOUR_CLOCK;
        bits |= (Pref.getBooleanDefaultFalse("bluejay_collector_enabled") ? 1 : 0) << SET_BIT_RUN_COLLECTOR;
        //bits |= (Pref.getBooleanDefaultFalse("bluejay_delta_trend") ? 1 : 0) << SET_BIT_TREND_FROM_DELTA;
        //bits |= (Pref.getBooleanDefaultFalse("bluejay_timing_failsafe") ? 1 : 0) << SET_BIT_FAILSAFE_TIMING;
        return bits;
    }

    private byte screenWakeByte() {
        byte bits = 0;
        bits |= (Pref.getBooleanDefaultFalse("bluejay_use_motion_wake") ? 1 : 0) << 7;
        bits |= getScreenTimeOutBits() << 4;
        bits |= getVelocityThresholdBits();
        return bits;
    }

    private byte getVelocityThresholdBits() {

        int value = (Pref.getInt("bluejay_wake_velocity", -1));
        if (value < 0 || value > 15) {
            value = 6; // default
        }
        return (byte) (value & 0xff);
    }

    private byte getScreenTimeOutBits() {
        int value = (Pref.getInt("bluejay_screen_timeout", -1));
        if (value < 0 || value > 7) {
            value = 6; // default
        }
        return (byte) ((7 - value) & 0xff);
    }


}
