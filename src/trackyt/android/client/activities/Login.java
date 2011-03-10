package trackyt.android.client.activities;

import trackyt.android.client.R;
import trackyt.android.client.TrackytApiAdapter;
import trackyt.android.client.TrackytApiAdapterFactory;
import trackyt.android.client.exceptions.NotAuthenticatedException;
import trackyt.android.client.models.ApiToken;
import trackyt.android.client.reponses.AuthenticationResponse;
import trackyt.android.client.utils.RequestMaker;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends Activity {

	private EditText loginEditText;
	private EditText passwordEditText;
	private Button loginButton;
	private Button createAccountButton;
	
	private TrackytApiAdapter mAdapter;
	private ApiToken token;
	private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		loginEditText = (EditText) findViewById(R.id.login_edit_text);
		passwordEditText = (EditText) findViewById(R.id.password_edit_text);
		loginButton = (Button) findViewById(R.id.login_button);
		createAccountButton = (Button) findViewById(R.id.create_account_button);
		mAdapter = TrackytApiAdapterFactory.createV11Adapter();

		loginEditText.setText("test@gmail.com");
		passwordEditText.setText("m");
		
	}

	public void loginOnClick(View view) {
		new LoginMe().execute();
    }

	public void createAccountOnClick(View view) {
		Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();

		// TODO: implement createAccountOnClick()
	}

	private void openTasksBoardActivity() {
		Intent intent = new Intent(Login.this, TasksBoard.class);
		intent.putExtra("token", token.getToken());
		Login.this.startActivity(intent);
	}
	
	private class LoginMe extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... v) {
			try {
				publishProgress();
				token = mAdapter.authenticate(loginEditText.getText().toString(), passwordEditText.getText().toString()); 
			} catch (NotAuthenticatedException e) {
				return false;
			} finally {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
			}
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Void... v) {
			super.onProgressUpdate(v);
			progressDialog = ProgressDialog.show(Login.this, "", "Loging in. Please wait...");
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				openTasksBoardActivity();
			} else {
				Toast.makeText(getApplicationContext(), "Login wasn't successful, try again", Toast.LENGTH_SHORT).show();
			}
		}
	}

}