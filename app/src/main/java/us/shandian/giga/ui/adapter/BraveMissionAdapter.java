package us.shandian.giga.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;

import org.schabi.newpipe.App;
import org.schabi.newpipe.R;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import us.shandian.giga.get.Mission;
import us.shandian.giga.service.DownloadManager;

// this is sponsorblock related stuff
public abstract class BraveMissionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected void braveSponsorOpenHelper(final DownloadManager.MissionItem item) {
        final Context context = App.getInstance();
        final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (mPrefs.getBoolean(context
                .getString(R.string.enable_local_player_key), false)) {
            braveOpen(item.mission);
        } else {
            braveOpenExternally(item.mission);
        }
    }

    protected boolean braveSponsorBlockOpenExternally(int id, Mission mission) {
        if (id == R.id.open_externally) {
            braveOpenExternally(mission);
            return true;
        }
        return false;
    }

    // the abstract method is only to mark what belongs to sponsorblock
    protected abstract void braveOpen(Mission mission);

    // the abstract method is only to mark what belongs to sponsorblock
    protected abstract void braveOpenExternally(Mission mission);
}
