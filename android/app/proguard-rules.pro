# Manifest components (Application, Activity, Service, Receivers) are kept by
# the rules AGP generates from the merged manifest. These are the two classes
# the framework instead builds reflectively, by constructor signature.
-keep class com.basemax.smsforwarder.work.SyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.basemax.smsforwarder.ui.MainViewModel {
    public <init>(android.app.Application);
}

# Flatten every remaining class into the root package. R8 full mode already
# renames them; this also collapses the package strings in the dex.
-repackageclasses ''
