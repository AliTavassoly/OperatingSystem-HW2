package os.hw2.storage;

import os.hw2.util.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class MasterHandler {
    private ServerSocket storageServerSocket;
    private PrintStream masterPrintStream;
    private Scanner masterScanner;

    public MasterHandler(ServerSocket storageServerSocket) {
        this.storageServerSocket = storageServerSocket;

        connectToMaster();
    }

    public void connectToMaster() {
        try {
            Socket socket = storageServerSocket.accept();
            masterPrintStream = new PrintStream(socket.getOutputStream());
            masterScanner = new Scanner(socket.getInputStream());

            Logger.getInstance().log("Master connected to Storage");
        } catch (
        IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeMemory(ArrayList<Integer> memory) {
        String memoryString = masterScanner.nextLine();

        String[] listOfData = memoryString.split(" ");

        for (int i = 0; i < listOfData.length; i++)
            memory.add(Integer.parseInt(listOfData[i]));

        Logger.getInstance().log("Memory initialized with: " + memory);
    }
}
