package trackyt.android.client.ui.phone;

import java.util.List;

import trackyt.android.client.R;
import trackyt.android.client.TrackytApiAdapterFactory;
import trackyt.android.client.controller.TimeController;
import trackyt.android.client.models.ApiToken;
import trackyt.android.client.models.Task;
import trackyt.android.client.ui.dialog.ADialog;
import trackyt.android.client.utils.RequestMaker;
import android.app.ActivityGroup;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class TasksBoard extends ActivityGroup implements TasksScreen {
	private static final String TAG = "TasksBoard";
	public static String token;
	public static TimeController timeController;
	public static RequestMaker requestMaker;
	public static List<Task> taskList;

	private MyAdapter mAdapter;
	private ListView listView;
	private EditText editText;
	private ProgressDialog progressDialog;
	private ADialog alert;
	private TabHost mTab;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tasks_board);

		Bundle extras = getIntent().getExtras();
		token = (String) extras.get("token");

		timeController = new TimeController(this,
				TrackytApiAdapterFactory.createV11Adapter(),
				new ApiToken(token));

		listView = (ListView) findViewById(R.id.list_view);
		editText = (EditText) findViewById(R.id.edit_text);
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				new AddNewTask().execute();
				return true;
			}
		});
		
		mTab = (TabHost) findViewById(R.id.tabhost);
		TabHost.TabSpec spec = mTab.newTabSpec("All");
		mTab.setup(this.getLocalActivityManager());

		spec.setContent(R.id.all_tab);
		spec.setIndicator("All", getResources().getDrawable(R.drawable.emo_im_cool));
		mTab.addTab(spec);
		
		Intent intent = new Intent().setClass(this, TasksDone.class);
		spec = mTab.newTabSpec("Done");
		spec.setContent(intent);
		spec.setIndicator("Done", getResources().getDrawable(R.drawable.emo_im_happy));
		mTab.addTab(spec);
		
		new TasksLoader().execute();
		alert = new ADialog(this, timeController, this);
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
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		
		int tab = mTab.getCurrentTab();
		if (tab == 0) {
			inflater.inflate(R.menu.taskboard_menu, menu);
		} else {
			inflater.inflate(R.menu.tasks_done_menu, menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			new TasksLoader().execute();
			return true;
		case R.id.menu_done_refresh:
			TasksDone td = (TasksDone)this.getCurrentActivity();
			td.newTasksLoader();
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
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(50);
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

			listView.setOnItemClickListener(mListener);

			progressDialog = null;
		}
	}
	
	private AdapterView.OnItemClickListener mListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,
				int position, long arg3) {
			Task task = (Task) listView.getItemAtPosition(position);
			alert.setTask(task);
			alert.show();
		}
	};

	private class AddNewTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
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
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(50);
				Toast.makeText(getApplicationContext(),
						"Task hasn't been created, try again",
						Toast.LENGTH_SHORT);
			}
		}

	}

	@Override
	public void freezeViews() {
		listView.setOnItemClickListener(null);
		Log.d(TAG, "freezeViews()");
	}

	@Override
	public void unfreezeViews() {
		listView.setOnItemClickListener(mListener);
		Log.d(TAG, "unfreezeViews()");
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.d("Dev", "onRetainNonConfigurationInstance");
		return super.onRetainNonConfigurationInstance();
	}
}