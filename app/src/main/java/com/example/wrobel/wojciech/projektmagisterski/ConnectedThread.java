package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Handler;

import com.github.pires.obd.commands.ObdMultiCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.control.DtcNumberCommand;
import com.github.pires.obd.commands.control.EquivalentRatioCommand;
import com.github.pires.obd.commands.control.IgnitionMonitorCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.control.PendingTroubleCodesCommand;
import com.github.pires.obd.commands.control.PermanentTroubleCodesCommand;
import com.github.pires.obd.commands.control.TimingAdvanceCommand;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.exceptions.NoDataException;

public class ConnectedThread extends Thread {
    private static final String TAG = "ConnectedThread";
    public static final int MESSAGE_READ = 2;

    private final BluetoothSocket mSocket;
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    private final Handler mHandler;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        Log.d(TAG, "ConnectedThread: Constructor");
        mHandler = handler;
        mSocket = socket;
        InputStream tmpIS = null;
        OutputStream tmpOS = null;
        try{
            tmpIS = socket.getInputStream();
            tmpOS= socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Not able to create input output streams", e);
        }
        mInputStream = tmpIS;
        mOutputStream = tmpOS;
    }


    public void run() {
        Log.d(TAG, "Start ConnectedThread");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                getAllOBDreadings();
                sleep(100);

            } catch (IOException e) {
                Log.e(TAG, "Disconnected", e);
                MainActivity.mBluetoothIO.connectionLost();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NoDataException e){
                e.printStackTrace();
            }
        }
    }

    private void getAllOBDreadings() throws IOException, InterruptedException, NoDataException {
        MyMultiOBDCommand multiCommand = new MyMultiOBDCommand(mSocket, mHandler);

        multiCommand.sendCommands();
        Log.d(TAG, "Control: "+ multiCommand.getFormattedResult());
    }

    void cancel() {
        try {
            mInputStream.close();
            mOutputStream.close();
            if(mSocket!=null){
                mSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "cancel: close() of connected failed", e);
        }
    }
}
