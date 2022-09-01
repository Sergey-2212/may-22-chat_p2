package ru.gb.may_chat.client.history;

import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

public class HistoryController {

    private String nickname;

    private BufferedWriter writer;

    private BufferedReader reader;

    Path historyPath;

    public HistoryController(String nickname) {
        this.nickname = nickname;

        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        this.historyPath = Path.of(String.format(
                "C:\\Users\\Professional\\Documents\\Программирование\\GeekBrains\\1Четверть\\may-22-chat_p2\\chat-client\\src\\main\\java\\ru\\gb\\may_chat\\client\\history\\files\\%s.log",nickname));
        if(!Files.exists(historyPath)) {
            Files.createFile(historyPath);
        }
        reader  = Files.newBufferedReader(historyPath);
        writer = Files.newBufferedWriter(historyPath, StandardOpenOption.APPEND);
    }

    public void writeBroadcastmsg(String msg, String sender) throws IOException {
        writer.write(String.format("[%s]: %s",sender, msg));
        writer.newLine();
    }

    public void writePrivatemsg(String msg, String sender, String recepient) throws IOException {
        writer.write(String.format("[%s]=>[%s]: %s",sender, recepient, msg));
        writer.newLine();
    }

    public void writeInputmsg(String msg) throws IOException {
        writer.write(msg);
        writer.newLine();
    }

    public void stop() throws IOException {
        writer.flush();
        writer.close();
    }

    public void changeFilename(String oldNickname, String newNickname) {
        Path oldNick = Path.of(String.format("C:\\Users\\Professional\\Documents\\Программирование\\GeekBrains\\1Четверть\\may-22-chat_p2\\chat-client\\src\\main\\java\\ru\\gb\\may_chat\\client\\history\\files\\%s.log", oldNickname));
        Path newNick = Path.of(String.format("C:\\Users\\Professional\\Documents\\Программирование\\GeekBrains\\1Четверть\\may-22-chat_p2\\chat-client\\src\\main\\java\\ru\\gb\\may_chat\\client\\history\\files\\%s.log", newNickname));

        try {
            Files.move(oldNick, newNick);
            historyPath = newNick;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("changeFilename haven't worked recently.");
        }
    }

    public String getlasthistory(int numberOfStrings) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        ReversedLinesFileReader object = new ReversedLinesFileReader(historyPath.toFile());
            for (int i = 0; i < numberOfStrings; i++) {
                String line = object.readLine();
                if (line == null)
                    break;
                stringBuilder.append(String.format("%s\n",line));
            }
            return stringBuilder.toString();
    }

}


