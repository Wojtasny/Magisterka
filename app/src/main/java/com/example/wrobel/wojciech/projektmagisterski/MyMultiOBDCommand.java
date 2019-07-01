package com.example.wrobel.wojciech.projektmagisterski;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
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
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.NoDataException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MyMultiOBDCommand extends ObdMultiCommand {

    private static final String TAG = "MyMultiOBDCommand";
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ArrayList<ObdCommand> commandsControl;
    private ArrayList<ObdCommand> commandsEngine;
    private ArrayList<ObdCommand> commandsFuel;
    private ArrayList<ObdCommand> commandsPreassure;
    private ArrayList<ObdCommand> commandsTemparature;
    private Handler mHandler;

    public void sendCommands() throws IOException, InterruptedException {

        actuallySend(commandsControl);
        actuallySend(commandsEngine);
        actuallySend(commandsFuel);
        actuallySend(commandsPreassure);
        actuallySend(commandsTemparature);
    }

    private void actuallySend(ArrayList<ObdCommand> commands) throws IOException, InterruptedException {
        for(ObdCommand command: commands){
            try{
                command.run(mInputStream, mOutputStream);
            } catch (NoDataException | MisunderstoodCommandException e) {
                Log.d(TAG, "sendCommands: "+ command.getName() + " - NO DATA");
            }
        }
    }

    public MyMultiOBDCommand(BluetoothSocket socket, Handler handler) {
        try {
            this.mInputStream = socket.getInputStream();
            this.mOutputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "MyMultiOBDCommand: errors getting input and output streams");
        }
        this.mHandler = handler;
        prepareCommands();
    }

    private void prepareCommands() {
        commandsControl = new ArrayList<>();
        commandsControl.add(new DistanceMILOnCommand());
        commandsControl.add(new DistanceSinceCCCommand());
        commandsControl.add(new DtcNumberCommand());
        commandsControl.add(new EquivalentRatioCommand());
        commandsControl.add(new IgnitionMonitorCommand());
        commandsControl.add(new ModuleVoltageCommand());
        commandsControl.add(new PendingTroubleCodesCommand());
        commandsControl.add(new PermanentTroubleCodesCommand());
        commandsControl.add(new TimingAdvanceCommand());
        commandsControl.add(new TroubleCodesCommand());
        commandsControl.add(new VinCommand());
        commandsControl.add(new SpeedCommand());

        commandsEngine = new ArrayList<>();
        commandsEngine.add(new AbsoluteLoadCommand());
        commandsEngine.add(new LoadCommand());
        commandsEngine.add(new MassAirFlowCommand());
        commandsEngine.add(new OilTempCommand());
        commandsEngine.add(new RPMCommand());
        commandsEngine.add(new RuntimeCommand());
        commandsEngine.add(new ThrottlePositionCommand());

        commandsFuel = new ArrayList<>();
        commandsFuel.add(new AirFuelRatioCommand());
        commandsFuel.add(new ConsumptionRateCommand());
        commandsFuel.add(new FindFuelTypeCommand());
        commandsFuel.add(new FuelLevelCommand());
        commandsFuel.add(new WidebandAirFuelRatioCommand());

        commandsPreassure = new ArrayList<>();
        commandsPreassure.add(new BarometricPressureCommand());
        commandsPreassure.add(new FuelPressureCommand());
        commandsPreassure.add(new FuelRailPressureCommand());
        commandsPreassure.add(new IntakeManifoldPressureCommand());

        commandsTemparature = new ArrayList<>();
        commandsTemparature.add(new AirIntakeTemperatureCommand());
        commandsTemparature.add(new AmbientAirTemperatureCommand());
        commandsTemparature.add(new EngineCoolantTemperatureCommand());
    }

    private StringBuilder getResultFromCommands(ArrayList<ObdCommand> commands){
        StringBuilder res = new StringBuilder();
        Message msg;
        for (ObdCommand command : commands){
            String formattedResult = command.getFormattedResult();
            if(formattedResult.equals("")){
                formattedResult = "No Data";
            }
            res.append(formattedResult).append(",");
            msg = mHandler.obtainMessage(Menu_Activity.MESSAGE_READ);
            Bundle bundle = new Bundle();
            bundle.putString(Menu_Activity.FORMATTED_VALUE, formattedResult);
            bundle.putString(Menu_Activity.FORMATTED_VALUE_CLASS_NAME, command.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        return res;
    }
    @Override
    public String getFormattedResult() {
        StringBuilder res = new StringBuilder();
        res.append(getResultFromCommands(commandsControl));
        res.append(getResultFromCommands(commandsEngine));
        res.append(getResultFromCommands(commandsFuel));
        res.append(getResultFromCommands(commandsPreassure));
        res.append(getResultFromCommands(commandsTemparature));
        return res.toString();
    }
}
