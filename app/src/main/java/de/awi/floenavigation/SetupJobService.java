package de.awi.floenavigation;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class SetupJobService extends JobService {
    private static final String TAG = "InitialSetupJobService";

    @Override
    public boolean onStartJob(JobParameters params){
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params){
        return false;
    }
}
