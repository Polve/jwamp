/**
 *Copyright [2012] [Ghetolay]
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package com.github.ghetolay.jwamp.rpc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ghetolay.jwamp.WampConnection;
import com.github.ghetolay.jwamp.WampMessageHandler;
import com.github.ghetolay.jwamp.message.BadMessageFormException;
import com.github.ghetolay.jwamp.message.WampCallErrorMessage;
import com.github.ghetolay.jwamp.message.WampCallMessage;
import com.github.ghetolay.jwamp.message.WampCallResultMessage;
import com.github.ghetolay.jwamp.message.WampMessage;

/**
 * @author ghetolay
 *
 */
public abstract class AbstractRPCManager implements WampMessageHandler{

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private WampConnection conn;
	
	private ExecutorService executor = Executors.newFixedThreadPool(25);
	
	public void onConnected(WampConnection connection) {
		conn = connection;
	}

	public void onClose(String sessionId, int closeCode) {
		conn = null;
	}
	
	public boolean onMessage(String sessionId, WampMessage message) throws BadMessageFormException {
		
		if(message.getMessageType() == WampMessage.CALL){
			RunnableAction action = getRunnableAction(sessionId, (WampCallMessage)message);
			if(action != null){
				executor.execute(action);
				return true;
			}
		}
		return false;
	}
	
	protected void sendResult(String callId, Object result) throws IOException{
		WampCallResultMessage resultMsg;

		if(result != null && result instanceof MultipleResult){
			MultipleResult r = (MultipleResult)result;

			resultMsg = new WampCallResultMessage(r.isLast());
			resultMsg.setResult(r.getResult());	
		}else{
			resultMsg = new WampCallResultMessage();
			resultMsg.setResult(result);	
		}	

		resultMsg.setCallId(callId);

		conn.sendMessage(resultMsg);
	}

	protected void sendError(String callId, String procId, Throwable e) throws IOException{
		WampCallErrorMessage errorMsg = new WampCallErrorMessage(callId, procId,e.getLocalizedMessage());
		if(!e.getMessage().equals(e.getLocalizedMessage()))
			errorMsg.setErrorDetails(e.getMessage());

		conn.sendMessage(errorMsg);
	}

	protected abstract RunnableAction getRunnableAction(String sessionId, WampCallMessage message);
	
	public static abstract class RunnableAction implements Runnable{

		protected String sessionId;
		protected WampCallMessage message;
		
		public RunnableAction(String sessionId, WampCallMessage message) {
			this.sessionId = sessionId;
			this.message = message;
		}
		
	}
}


