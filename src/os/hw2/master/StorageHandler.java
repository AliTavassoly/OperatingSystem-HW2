package os.hw2.master;

import os.hw2.Main;
import os.hw2.Task;
import os.hw2.util.Message;
import os.hw2.util.Logger;
import os.hw2.util.MyGson;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class StorageHandler {
    private Socket storageSocket;
    private PrintStream storagePrintStream;
    private Scanner storageScanner;

    private int storagePort;

    private Process process;

    public StorageHandler(int storagePort) {
        this.storagePort = storagePort;

        connectToStorage();
    }

    private void createStorageProcess() {
        try {
            process = new ProcessBuilder(
                    Main.commonArgs[0], Main.commonArgs[1], Main.commonArgs[2], Main.commonArgs[3], Main.commonArgs[4],
                    "os.hw2.storage.Storage",
                    String.valueOf(storagePort), String.valueOf(Main.numberOfWorkers)
            ).start();

            Logger.getInstance().log("Storage process created, PID: " + process.pid() + ", Port: " + storagePort);

            startErrorListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startErrorListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(process.getErrorStream());
            while (true) {
                System.out.println(scanner.nextLine());
            }
        }).start();
    }

    private void connectToStorage() {
        try {
            createStorageProcess();

            // Wait until process creation
            Thread.sleep(100);

            storageSocket = new Socket(InetAddress.getLocalHost(), storagePort);

            storagePrintStream = new PrintStream(storageSocket.getOutputStream());
            storageScanner = new Scanner(storageSocket.getInputStream());

            sendInitialMemory();

            sendDeadlock();

            sendAllTasks();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendInitialMemory() {
        storagePrintStream.println(Main.memoryString);
        storagePrintStream.flush();
    }

    private void sendAllTasks() {
        storagePrintStream.println(Main.taskNumber);
        storagePrintStream.flush();

        for(int i = 0; i < Main.taskNumber; i++) {
            storagePrintStream.println(MyGson.getGson().toJson(Main.tasks[i]));
            storagePrintStream.flush();
        }
    }

    private void sendDeadlock() {
        storagePrintStream.println(MyGson.getGson().toJson(Main.deadlock));
        storagePrintStream.flush();
    }

    public void sendMessage(Message message) {
        storagePrintStream.println(MyGson.getGson().toJson(message));
        storagePrintStream.flush();
    }

    public void unlock(Task task) {
        Message message = new Message();
        message.setType(Message.Type.UNLOCK);
        message.setTask(task);

        sendMessage(message);
    }

    public void shutDown() {
        process.destroy();
        try {
            storageSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFromWaiters(Task task) {
        Message message = new Message();
        message.setType(Message.Type.REMOVE_WAITER);
        message.setTask(task);

        sendMessage(message);
    }

    public void askTaskPermissionFromStorage(int taskID) {
        Message message = new Message();
        message.setType(Message.Type.DEADLOCK_STATE);
        message.setTaskID(taskID);
        sendMessage(message);
    }

    public boolean getPermissionAnswer() {
        Message message = MyGson.getGson().fromJson(storageScanner.nextLine(), Message.class);
        Logger.getInstance().log("New message from storage " + message);
        return message.getPermission();
    }
}
