/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.net.simples;

import co.rsk.net.MessageHandler;
import co.rsk.net.MessageSender;
import co.rsk.net.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by ajlopez on 5/15/2016.
 */
public class SimpleAsyncNode extends SimpleNode {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinkedBlockingQueue<Future> futures = new LinkedBlockingQueue<>(1000);

    public SimpleAsyncNode(MessageHandler handler) {
        super(handler);
    }

    @Override
    public void processMessage(MessageSender sender, Message message) {
        futures.add(executor.submit(() -> {
            MessageTask task = new MessageTask(sender, message);
            task.execute(this.getHandler());
        }));
    }

    public void joinWithTimeout() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public void waitUntilNTasksWithTimeout(int number) {
        try {
            for (int i = 0; i < number; i++) {
                Future task = this.futures.poll(10, TimeUnit.SECONDS);
                if (task == null) {
                    throw new RuntimeException("Exceeded waiting time");
                }
                task.get();
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }

    private class MessageTask {
        private MessageSender sender;
        private Message message;

        public MessageTask(MessageSender sender, Message message) {
            this.sender = sender;
            this.message = message;
        }

        public void execute(MessageHandler handler) {
            handler.processMessage(sender, message);
        }
    }
}
