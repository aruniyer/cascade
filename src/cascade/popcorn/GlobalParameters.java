package cascade.popcorn;

public interface GlobalParameters {
	
	abstract class FileParameters { 
		public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
		public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
		public static String[] trainLocations = new String[time.length];
		public static String[] testLocations = new String[time.length];
		static {
			for (int i = 0; i < trainLocations.length; i++) {
				trainLocations[i] = prefix + "Train/At" + time[i] + ".csv";
			}
			for (int i = 0; i < trainLocations.length; i++) {
				testLocations[i] = prefix + "Test/At" + time[i] + ".csv";
			}
		}
	}
	
	abstract class SAROCFileParameters { 
		public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
		public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
		public static String[] trainLocations = new String[time.length];
		public static String[] testLocations = new String[time.length];
		static {
			for (int i = 0; i < trainLocations.length; i++) {
				trainLocations[i] = prefix + "Train/sarocAt" + time[i] + ".csv";
			}
			for (int i = 0; i < trainLocations.length; i++) {
				testLocations[i] = prefix + "Test/sarocAt" + time[i] + ".csv";
			}
		}
	}
	
	abstract class SAFileParameters { 
		public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
		public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
		public static String[] trainLocations = new String[time.length];
		public static String[] testLocations = new String[time.length];
		static {
			for (int i = 0; i < trainLocations.length; i++) {
				trainLocations[i] = prefix + "Train/saAt" + time[i] + ".csv";
			}
			for (int i = 0; i < trainLocations.length; i++) {
				testLocations[i] = prefix + "Test/saAt" + time[i] + ".csv";
			}
		}
	}
	
	abstract class SizeFileParameters { 
        public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
        public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
        public static String[] trainLocations = new String[time.length];
        public static String[] testLocations = new String[time.length];
        static {
            for (int i = 0; i < trainLocations.length; i++) {
                trainLocations[i] = prefix + "Train/sizeAt" + time[i] + ".csv";
            }
            for (int i = 0; i < trainLocations.length; i++) {
                testLocations[i] = prefix + "Test/sizeAt" + time[i] + ".csv";
            }
        }
    }
	
	abstract class WeinerFileParameters { 
        public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
        public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
        public static String[] trainLocations = new String[time.length];
        public static String[] testLocations = new String[time.length];
        static {
            for (int i = 0; i < trainLocations.length; i++) {
                trainLocations[i] = prefix + "Train/weinerAt" + time[i] + ".csv";
            }
            for (int i = 0; i < trainLocations.length; i++) {
                testLocations[i] = prefix + "Test/weinerAt" + time[i] + ".csv";
            }
        }
    }
	
	abstract class DiffFileParameters {
		public static String prefix = "/home/aruniyer/Workspace/Data/meme_data_for_HMM/";
		public static String[] time = { "1", "2", "3", "4", "5", "6", "7", "8" };
		public static String[] trainLocations = new String[time.length];
		public static String[] testLocations = new String[time.length];
		static {
			for (int i = 0; i < trainLocations.length; i++) {
				trainLocations[i] = prefix + "diffTrain/At" + time[i] + ".csv";
			}
			for (int i = 0; i < trainLocations.length; i++) {
				testLocations[i] = prefix + "diffTest/At" + time[i] + ".csv";
			}
		}
	}

}
