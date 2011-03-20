package trackyt.android.client.activities;

import java.util.List;

import trackyt.android.client.R;
import trackyt.android.client.TrackytApiAdapterFactory;
import trackyt.android.client.controller.TimeController;
import trackyt.android.client.models.ApiToken;
import trackyt.android.client.models.Task;
import trackyt.android.client.utils.RequestMaker;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TasksBoard extends Activity implements TasksScreen {
	List<Task> taskList;
	RequestMaker requestMaker;

	MyAdapter mAdapter;

	ListView listView;
//	Button okButton;
	EditText editText;

	MDialog itemPressDialog;

	TimeController timeController;
	ProgressDialog progressDialog;

	ADialog alert;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tasks_board);

		Bundle extras = getIntent().getExtras();
		String token = (String) extras.get("token");

		timeController = new TimeController(this,
				TrackytApiAdapterFactory.createV11Adapter(),
				new ApiToken(token));
		itemPressDialog = new MDialog(timeController, this);

		listView = (ListView) findViewById(R.id.list_view);

		new TasksLoader().execute();
		alert = new ADialog(this, timeController);
	}

	@Override
	public List<Task> getTaskList() {
		return taskList;
	}

	@Override
	public void updateUI() {
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mAdapter != null)
			updateUI();
	}

	public void onClickOKButton(View view) {

		new AddNewTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.taskboard_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.start_all:
			new StartAll().execute();
			return true;
		case R.id.stop_all:
			new StopAll().execute();
			return true;
		case R.id.menu_refresh:
			new TasksLoader().execute();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupListView() {
		mAdapter = new MyAdapter(this, R.id.list_view, taskList);
		listView.setAdapter(mAdapter);
		listView.setCacheColorHint(Color.WHITE);
	}

	private class MyAdapter extends ArrayAdapter<Task> {
		private LayoutInflater mInflater;

		public MyAdapter(Context context, int resource, List<Task> list) {
			super(context, resource, list);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ViewHolder viewHolder;

			Task task = taskList.get(position);
			
			if (v == null) {
				v = mInflater.inflate(R.layout.list_item, null);
				viewHolder = new ViewHolder(v);
				v.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) v.getTag(); 
			}
			
			viewHolder.populateFrom(task);

			return v;
		}

	}

	static class ViewHolder {
		private TextView description;
		private TextView time;
		private View view;

		ViewHolder(View view) {
			this.view = view;
			description = (TextView) view.findViewById(R.id.task_text_view);
			time = (TextView) view.findViewById(R.id.time_text_view);
		}

		void populateFrom(Task task) {
			description.setText(task.getDescription());
			time.setText(task.showTime());
		}
	}

	private class TasksLoader extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {

			try {
				publishProgress();
				taskList = timeController.loadTasks();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
			}

		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			progressDialog = ProgressDialog.show(TasksBoard.this, "",
					"Loading tasks. Please wait...");
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			if (!result) {
				Toast.makeText(getApplicationContext(),
						"Something wrong happened, try again",
						Toast.LENGTH_SHORT).show();
				return;
			}

			setupListView();
			updateUI();
			if (!timeController.getCounterFlag()) {
				timeController.runCount();
			}

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					Task task = (Task) listView.getItemAtPosition(position);
					alert.setTask(task);
					alert.show();
				}
			});

			progressDialog = null;
		}
	}

	private class AddNewTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
//			okButton = (Button) findViewById(R.id.ok_button);
			editText = (EditText) findViewById(R.id.edit_text);

			String taskDescription = editText.getText().toString();
			Task task = new Task(taskDescription);
			task.parseTime();
			publishProgress();
			try {
				timeController.addNewTask(task.getDescription());
				return true;
			} catch (Exception e) {
				taskList.remove(task);
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			Toast.makeText(getApplicationContext(), "Adding new task...",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				new TasksLoader().execute();
				updateUI();
				editText.setText("");
				Toast.makeText(getApplicationContext(), "Task created",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"Task hasn't been created, try again",
						Toast.LENGTH_SHORT);
			}
		}

	}

	private class StartAll extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			publishProgress();
			try {
				timeController.startAll();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			Toast.makeText(getApplicationContext(), "Starting all tasks...",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				Toast.makeText(getApplicationContext(), "Tasks started",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"Task hasn't been started, try again",
						Toast.LENGTH_SHORT);
			}
		}
	}

	private class StopAll extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			publishProgress();
			try {
				timeController.stopAll();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			Toast.makeText(getApplicationContext(), "Stoping all tasks...",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				Toast.makeText(getApplicationContext(), "Tasks stopped",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"Task hasn't been stopped, try again",
						Toast.LENGTH_SHORT);
			}
		}

	}
}