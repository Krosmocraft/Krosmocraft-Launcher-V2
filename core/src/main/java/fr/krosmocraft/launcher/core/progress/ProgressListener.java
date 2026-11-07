package fr.krosmocraft.launcher.core.progress;

public interface ProgressListener {

	ProgressListener NONE = new ProgressListener() {
		@Override
		public void step(String key) {
		}

		@Override
		public void progress(long done, long total) {
		}
	};

	void step(String key);

	void progress(long done, long total);
}
