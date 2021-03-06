package trackyt.android.client.adapter;

import java.util.List;

import org.apache.http.HttpException;

import trackyt.android.client.TrackytApiAdapter;
import trackyt.android.client.exceptions.NotAuthenticatedException;
import trackyt.android.client.models.ApiToken;
import trackyt.android.client.models.Task;
import trackyt.android.client.reponses.AuthenticationResponse;
import trackyt.android.client.reponses.BaseResponse;
import trackyt.android.client.reponses.CreateTaskResponse;
import trackyt.android.client.reponses.DeleteTaskResponse;
import trackyt.android.client.reponses.GetAllTasksResponse;
import trackyt.android.client.reponses.StartTaskResponse;
import trackyt.android.client.reponses.StopTaskResponse;
import trackyt.android.client.utils.MyConfig;
import trackyt.android.client.utils.RequestMaker;
import android.util.Log;

import com.google.gson.Gson;

public class ApiV11Adapter implements TrackytApiAdapter {

	private static final String TAG = "ApiV11Adapter.class";
	private RequestMaker requestMaker;

	public ApiV11Adapter() {
		requestMaker = RequestMaker.getInstance();
	}

	@Override
	public ApiToken authenticate(String email, String password)
			throws NotAuthenticatedException {
		
		if (MyConfig.DEBUG)
			Log.d(TAG, "authenticate()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.authenticate(email, password);
		} catch (HttpException e) {
			throw new NotAuthenticatedException();
		}
		
		AuthenticationResponse authResponse = new Gson().fromJson(receivedString, AuthenticationResponse.class);

		if (!authResponse.success) {
			throw new NotAuthenticatedException();
		}
		
		ApiToken apiToken = new ApiToken(authResponse.getApiToken());
		
		return apiToken;
	}

	@Override
	public List<Task> getAllTasks(ApiToken token) throws Exception {
		if (token == null) {
			throw new IllegalArgumentException("token can't be null");
		}
		
		if (MyConfig.DEBUG)
			Log.d(TAG, "getAllTasks()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.getAllTasks(token);
		} catch (HttpException e) {
			Log.d("Dev", "Request/Response from/to server was unsuccessful");
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		GetAllTasksResponse response = new Gson().fromJson(receivedString, GetAllTasksResponse.class);
		
		if (!response.success) {
			throw new Exception("Get all tasks operation was unsuccessful");
		}
		
		for (Task task : response.getTasksList()) {
			task.parseTime();
		}
		
		return response.getTasksList();
	}

	@Override
	public Task addTask(ApiToken token, String description) throws Exception {
		if (MyConfig.DEBUG)
			Log.d(TAG, "addTask()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.addTask(token, description);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		CreateTaskResponse response = new Gson().fromJson(receivedString, CreateTaskResponse.class);
		
		if (!response.success) {
			throw new Exception("Add task operation was unsuccessful");
		}
		
		response.getTask().parseTime();
		
		return response.getTask();
	}

	@Override
	public int deleteTask(ApiToken token, int taskId) throws Exception {
		if (MyConfig.DEBUG)
			Log.d(TAG, "deleteTask()");
		String receivedString;
		
		try {
			receivedString = requestMaker.deleteTask(token, taskId);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		DeleteTaskResponse response = new Gson().fromJson(receivedString, DeleteTaskResponse.class);
		
		if (!response.success) {
			throw new Exception("Delete task operation was unsuccessful");
		}
		
		return response.getTaskId();
	}

	@Override
	public Task startTask(ApiToken token, int taskId) throws Exception {
		if (MyConfig.DEBUG)
			Log.d(TAG, "startTask()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.startTask(token, taskId);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		StartTaskResponse response = new Gson().fromJson(receivedString, StartTaskResponse.class);
		
		if (!response.success) {
			throw new Exception("Start task operation was unsuccessful");
		}
		
		response.getTask().parseTime();
		
		return response.getTask();
	}

	@Override
	public Task stopTask(ApiToken token, int taskId) throws Exception {
		if (MyConfig.DEBUG)
			Log.d(TAG, "stopTask()");
		String receivedString;
		
		try {
			receivedString = requestMaker.stopTask(token, taskId);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		StopTaskResponse response = new Gson().fromJson(receivedString, StopTaskResponse.class);
		
		if (!response.success) {
			throw new Exception("Stop task operation was unsuccessful");
		}
		
		response.getTask().parseTime();
		
		return response.getTask();
	}

	@Override
	public void doneTask(ApiToken token, int taskId) throws Exception {
		if (MyConfig.DEBUG) Log.d(TAG, "doneTask()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.setDoneTask(token, taskId);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		BaseResponse response = new Gson().fromJson(receivedString, BaseResponse.class);
		
		if (!response.success) {
			throw new Exception("Mark task as Done operation was unsuccessful");
		}
	}

	@Override
	public List<Task> getDoneTasks(ApiToken token) throws Exception {
		if (MyConfig.DEBUG) Log.d(TAG, "getDoneTask()");
		
		String receivedString;
		
		try {
			receivedString = requestMaker.getDoneTask(token);
		} catch (HttpException e) {
			throw new Exception("Request/Response from/to server was unsuccessful");
		}
		
		GetAllTasksResponse response = new Gson().fromJson(receivedString, GetAllTasksResponse.class);
		
		if (!response.success) {
			throw new Exception("Load Done tasks operation was unsuccessful");
		}
		
		for (Task task : response.getTasksList()) {
			task.parseTime();
		}
		
		return response.getTasksList();
	}

}