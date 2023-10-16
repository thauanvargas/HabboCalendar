import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@ExtensionInfo(
        Title = "HabboCalendar",
        Description = "Get your calendar gifts automatically!",
        Version = "1.0",
        Author = "Thauan"
)

public class HabboCalendar extends ExtensionForm implements Initializable {
    public static HabboCalendar RUNNING_INSTANCE;

    public String offer = "";
    public boolean indexIsMinor = true;
    public int fixDay = 0;
    public Label labelInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
    public String host;

    public GAsync gAsync;

    @Override
    protected void onStartConnection() {
        gAsync = new GAsync(this);

        new Thread(() -> {
            wait(10000);
            try {
                getCalendarOffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            wait(1000);
            if(!Objects.equals(offer, "")) {

                LocalDate currentDate = LocalDate.now();
                int currentDay = currentDate.getDayOfMonth();

                if(indexIsMinor) {
                    currentDay = currentDay - fixDay;
                }else {
                    currentDay = currentDay + fixDay;
                }

                sendToServer(new HPacket("OpenCampaignCalendarDoor", HMessage.Direction.TOSERVER, offer, currentDay));

                HPacket packet = gAsync.awaitPacket(new AwaitingPacket("CampaignCalendarDoorOpened", HMessage.Direction.TOCLIENT, 2000));

                if(packet != null) {
                    if (packet.readBoolean()) {

                        Platform.runLater(() -> {
                            labelInfo.setText("You successfuly claimed today's GIFT");
                            labelInfo.setTextFill(Color.GREEN);
                        });

                        try {
                            calendarClaimed();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Platform.runLater(() -> {
                            labelInfo.setText("I've tried to claim, but I didn't manage, maybe you claimed it already?");
                            labelInfo.setTextFill(Color.RED);
                        });
                    }
                }else {
                    Platform.runLater(() -> {
                        labelInfo.setText("There was a issue can you contact the DEV?");
                        labelInfo.setTextFill(Color.RED);
                    });
                }
            }


        }).start();

    }


    @Override
    protected void onShow() {
        System.out.println("HabboCalendar Started!");
    }

    @Override
    protected void initExtension() {
        RUNNING_INSTANCE = this;

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);
        });

    }

    public void calendarClaimed() throws IOException {
        new Thread(() -> {
            try {
                URL url = new URL ("https://xeol.online/calendar-promo-claimed");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                String content = offer;
                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = content.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject json = new JSONObject(response.toString());
                }
            }catch (IOException e){
                System.out.println(e);
            }

        }).start();

    }

    public void getCalendarOffer() throws IOException {
        new Thread(() -> {
            try {

                URL url = new URL ("https://xeol.online/calendar-promo");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                String content = "";
                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = content.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject json = new JSONObject(response.toString());
                    if(json.getBoolean("success")) {
                        offer = json.getString("offer");
                        indexIsMinor = json.getBoolean("isMinor");
                        fixDay = json.getInt("fixDay");
                    }else {
                        offer = "";
                    }
                }
            }catch (IOException e){
                System.out.println(e);
            }

        }).start();

    }

    public void wait(int time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) { }
    }
}
