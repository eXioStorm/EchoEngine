/*
 ################################################################################################################################################
 ###########################################I could not find a reasonable way to set audio starting    ##########################################
 ########################################### position after completion for looping only a portion of   ##########################################
 ###########################################  a track for things such as BGM, your way to implement    ##########################################
 ###########################################   this feature is to append this task to a scheduler that ##########################################
 ###########################################    will set the start position after the track has played ##########################################
 ###########################################     e.g. get the length of the whole track, and once the  ##########################################
 ###########################################      track completes then you set the start position and  ##########################################
 ###########################################       play. and by scheduler I mean a loop that checks    ##########################################
 ###########################################        for values indicating that an event should happen. ##########################################
 ################################################################################################################################################
 */

package com.github.exiostorm.audio;

import static com.github.exiostorm.testing.Audio.IOUtil.ioResourceToByteBuffer;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.AL11.*;

import static org.lwjgl.openal.EXTThreadLocalContext.alcSetThreadContext;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
//import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbisInfo;


@SuppressWarnings("deprecation")//MultiValueMap was replaced by MultiValuedMap, however I couldn't quickly figure out how to iterate through MultiValuedMap...
//TODO Put another dime in the jukebox baby easter egg console log when playing background music with the streaming function we haven't added yet.
// also love how the xylophone sounds in AOT soundtracks.
public class JukeBox {
	static long nextClean;//Used for scheduling the deletion of new sources by storing the nearest expiration time.
	//I dont think I really need these values... I'll review them later.
    static String file;
    String category;
	String reference;
	//
	static String errorMessage;//stores the string for error messages?
	float number;//dont think I need this. I'll review it later.
	static Integer refData;//dont think I need this. I'll review it later.
	boolean isCategory;//dont think I need this. I'll review it later.
	static boolean JBInit;
	static boolean reoccuring;//dont think I need this. I'll review it later.
    static long dev;//used to set up speaker device
    static long ct;//used to create context
	private static HashMap<String, String> soundKeys;//used to store all keys for sources? I dont think I need this... I'll review it later.
	private static HashMap<String, Integer> pauseState;//used to memorize which sounds are paused
	private static HashMap<String, Integer> sources;//used to store all created sources
	private static HashMap<Integer, String> secruos;//inverted sources to fetch the key String...
	private static MultiValueMap<String, Integer> categories;//used to assign sources to a category. one category string which is the key, and all sources in that category get paired with that key.
	private static MultiValueMap<String, Integer> instances;//used to keep duplicate sounds cleaned up (needed to allow them to overlap)
	private static HashMap<String, Integer> buffers;//stores buffers to the reference keys
	private static HashMap<Integer, Long> soundTime;//used to memorize when sources should expire.
	private static HashMap<String, Integer> soundClean;//used to memorize values to remove after the cleaner runs.
	public static boolean initialized;//planned to be used in initCheck() to make sure the program has initialized to prevent crashing.
	/**
     * this is a method to indicate if the program is initialized yet.
     */
	public static void initCheck() {
		if (!initialized) {
			System.out.println("Sound hasn't been Initialized yet!");
		}
	}
	/**
     * this is a method to indicate if the reference key is already being used.
     * @return returns true if the reference already exists, false if it doesnt.
     */
	public static boolean loadCheck(String reference) {
		if (sources.containsKey(reference)) {
			System.out.println("Reference exists already.");
			return true;
		}else {
			return false;
		}
		
	}
	/**
     * pretty much a duplicate of loadCheck with opposite output, I created it because I ended up confusing myself when I tried to just use one method.
     * @return returns true if the reference exists, false if it doesn't.
     */
	public static boolean playCheck(String reference) {
		if (!sources.containsKey(reference)) {
			System.out.println("Reference does not exist.");
			return false;
		}else {
			return true;
		}
		
	}
	/**
     * this is a method to indicate if the file exists or not. (and is used to prevent the program from crashing if you happen to try using something which doesn't exist.)
     */
	public static boolean pathCheck(String file) {
		String CurrentDir = System.getProperty("user.dir");
		String relativePath = CurrentDir + "/" + file;
		File fPath = new File(relativePath);
		if (fPath.exists()) {
			return true;
		}else {
			errorMessage = "Check your file path for ''" + relativePath + "'' we can't seem to find it!";
			System.out.println(errorMessage);
		return false;
		}
		
	}
	/**
     */
	static void checkALError() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            throw new RuntimeException(alGetString(err));
		}
    }
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//Initialize
	/**
     * this is the first method you run to use the program... it sets up all of the fields and services needed.
     */
	public static void Init() {
		nextClean = 1L;//sets nextClean to 1 so that the cleaning method can detect that no sounds have been created yet.
		pauseState = new HashMap<String, Integer>();
		sources = new HashMap<String, Integer>();//sets up the sources HashMap to be ready for use
		secruos = new HashMap<Integer, String>();
		soundClean = new HashMap<String, Integer>();
		categories = new MultiValueMap<String, Integer>();//sets up the categories MultiValueMap to be ready for use
		buffers = new HashMap<String, Integer>();//sets up the buffers HashMap to be ready for use
		soundKeys = new HashMap<String, String>();//sets up the soundKeys HashMap to be ready for use
		instances = new MultiValueMap<String, Integer>();//sets up the instances MultiValueMap to be ready for use
		soundTime = new HashMap<Integer, Long>();//sets up the soundTime HashMap to be ready for use
		initialized = true;//indicates that the Init() method has been run.
		//^^^^^^^^^^^^^
		//
		//^^^^^^^^^^^^^
		if (!JBInit) {//sets up the default speaker device to be used
			try {
				long device = alcOpenDevice((ByteBuffer)null);
				long context = alcCreateContext(device, (IntBuffer)null);
				ct = context;
				dev = device;
				ALCCapabilities deviceCaps = ALC.createCapabilities(device);
				System.out.println("OpenALC10: " + deviceCaps.OpenALC10);
				System.out.println("OpenALC11: " + deviceCaps.OpenALC11);
				System.out.println("caps.ALC_EXT_EFX = " + deviceCaps.ALC_EXT_EFX);
				String defaultDeviceSpecifier = Objects.requireNonNull(alcGetString(NULL, ALC_DEFAULT_DEVICE_SPECIFIER));
				System.out.println("Default device: " + defaultDeviceSpecifier);
				alcSetThreadContext(context);
				AL.createCapabilities(deviceCaps);
				JBInit = true;
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}else {
				System.out.println("Device is already open!");
				}
		}
//############################################################################################################
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//logic loops
	/**
     * this is the method used to set the position of the source, it will be called every time the position should change.
     * @param reference name of the sound
     * @param id id of the source of the sound (for use with reoccuring sounds)
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @param reoccuring is this a reoccuring sound? (is this a sound that gets duplicated)
     */
	public static void setPosition(String reference, int id, float x, float y, float z, boolean reoccuring) {
		if (playCheck(reference)){
			if (reoccuring){
				boolean i = true;
				int j = 1;
				while (i) {
					if(sources.get(reference+id+j) != null) {
						alSource3f(sources.get(reference+id+j), AL_POSITION, x, y, z);
						j++;
					}else {
						i = false;
					}
				}
				}else {
					alSource3f(sources.get(reference), AL_POSITION, x, y, z);
					}
			}else {
				playCheck(reference);
			}
	}
	//^^^^^^^^^^^^^
	/**
     * @param setListenerPosition this is the method used to set the position of the listener, it will be called every time the position of the listener changes.
     */
	public static void setListenerPosition(float x, float y, float z) {
		alListener3f(AL_POSITION, x, y, z);
	}
	//^^^^^^^^^^^^^
//############################################################################################################
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//What happens when file is loaded
/**
* this is the method used to load a file into memory.
* @param file the file path including the container
* @param category the category this sound belongs to
* @param reference  the name of this sound
*/
public static void load(String file, String category, String reference) {
	if(pathCheck(file)) {
		if (!loadCheck(reference)) {
			int bufferData = alGenBuffers();
			checkALError();
			int sourceData = alGenSources();
			checkALError();
			try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
				ShortBuffer pcm = readVorbis(file, 32 * 1024, info);
				alBufferData(bufferData, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
				checkALError();
				}
			alSourcei(sourceData, AL_BUFFER, bufferData);
			checkALError();
			sources.put(reference, sourceData);
			secruos.put(sourceData, reference);
			categories.put(category, sources.get(reference));
			buffers.put(reference, bufferData);
			soundKeys.put(reference, reference);
			}
		}
	}
//############################################################################################################	

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//This reads vorbis files. IE: .ogg
static ShortBuffer readVorbis(String resource, int bufferSize, STBVorbisInfo info) {
	ByteBuffer vorbis;
	try {
		vorbis = ioResourceToByteBuffer(resource, bufferSize);
		} catch (IOException e) {
			throw new RuntimeException(e);
			}
	IntBuffer error   = BufferUtils.createIntBuffer(1);
	long      decoder = stb_vorbis_open_memory(vorbis, error, null);
	if (decoder == NULL) {
		throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
		}
	stb_vorbis_get_info(decoder, info);
	int channels = info.channels();
    /**
     * @param lengthSamples int lengthSamples = stb_vorbis_stream_length_in_samples(decoder)<u> * channels</u>;
     */
    //This was the bug!! (audio ended sooner than it should)
    int lengthSamples = stb_vorbis_stream_length_in_samples(decoder) * channels;
    ShortBuffer pcm = BufferUtils.createShortBuffer(lengthSamples);
    pcm.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels);
    stb_vorbis_close(decoder);
    return pcm;
    }
//############################################################################################################

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
/**
 * this is the simplest method to use if you want to play audio (by sacrificing features)
 * @param reference the name you loaded the file with.
 */
public static void playi(String reference) {
	if (playCheck(reference)){
	alSourcePlay(sources.get(reference));
	checkALError();
	} else {
		playCheck(reference);
	}
}
//############################################################################################################

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//add part in here for handling the indexing of sounds. (if 1, then 2, else 1)
/**
 * this is the main method for playing sounds.
 * @param reference the name that you loaded the file with.
 * @param category the category that you want to put reoccuring sounds into.
 * @param id this is used to identify the creator of this sound, such as entities in your game.
 * @param overlapping this indicates if the sound should create new sources to allow them to overlap.
 */
public static void play(String reference, String category, int id, boolean overlapping) {
	if (playCheck(reference)){
		if (overlapping){
			clearOverlapping();
			long systime = System.currentTimeMillis();
			//System.out.println("current time: " + (systime));
			int sourceData = alGenSources();
			checkALError();
			int bufferData = buffers.get(reference);
			checkALError();
			alSourcei(sourceData, AL_BUFFER, bufferData);
			checkALError();
			//float var1 = (alGetSourcef(sourceData, AL_SEC_OFFSET)) * 1000;// this is the source of the bug, it always returns 0...
			long var1 = (long) (getLength(reference) * 1000);
			long var2 = (systime + var1);
			//long var3 = (long) var2;
			//long var4 = (long) ((alGetSourcef(sourceData, AL_SEC_OFFSET) * 1000) + systime);
			//System.out.println("sound length: " + (var1));
			long expiration = var2 + 100L;
			instances.put(reference, sourceData);
			soundTime.put(sourceData, expiration);
			int t = 1;
			//indexing will go here at sources.put / categories.put
			while(sources.get(reference+id+t)!=null) {
				t++;
				}
			sources.put(reference + id + t, sourceData);
			secruos.put(sourceData, reference + id + t);
			categories.put(category, reference + id + t);
			//System.out.println(nextClean + ": nextClean");
			//System.out.println(System.currentTimeMillis() + ": current time");
			if(expiration < nextClean || nextClean == 1L) {
				//System.out.println("expiration: " + expiration);
				nextClean = expiration;
				}
			alSourcePlay(sourceData);
			}
		if (!overlapping){
			alSourcePlay(sources.get(reference));
			checkALError();
			}
		}else {
			playCheck(reference);
			}
	}
//############################################################################################################
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
/**
 * this method is used to delete duplicate sources when they are no longer playing.
 */
private static void clearOverlapping(){
	if (nextClean < System.currentTimeMillis() && nextClean != 1L) {
		//iterate through soundTime and delete sources whos keys are smaller than the current system time, as well as the hashmap pair for those keys
		for(Integer key : soundTime.keySet()) {
			if(soundTime.get(key) < System.currentTimeMillis()) {
				int keyi = (int) key;
				//String keyString = secruos.get(keyi);//returns the value String
				//System.out.println("ClearOverlapping: deleting " + secruos.get(keyi) + "/" + key);
				alSourceStop(keyi);
				alDeleteSources(keyi);
				//sources.remove(keys);//whoops, trying to remove the value, needs to be key.
				categories.removeMapping(categories.containsValue(keyi), keyi);
				soundClean.put(secruos.get(keyi), key);
				sources.remove(secruos.get(keyi));
				secruos.remove(keyi);
				//this next value is the Concurrency error
				//soundTime.remove(key);
				}
			}
		for(String keyS : soundClean.keySet()) {
			soundTime.remove(soundClean.get(keyS));
		}
		for(int yek : soundTime.keySet()) {
			if (nextClean < soundTime.get(yek)) {
				nextClean = soundTime.get(yek);
			}
		}
		for(int yek2 : soundTime.keySet()) {
			if (nextClean > soundTime.get(yek2)) {
				nextClean = soundTime.get(yek2);
			}
		}
		soundClean.clear();
		if(nextClean < System.currentTimeMillis() && nextClean != 1L) {
			nextClean = 1L;
		}
	}
	else {}
}
//############################################################################################################

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
private static float getLength(String reference) {
	int buffer = buffers.get(reference);
	int bytes = AL10.alGetBufferi(buffer, AL10.AL_SIZE);
	int bits = AL10.alGetBufferi(buffer, AL10.AL_BITS);
	int channels = AL10.alGetBufferi(buffer, AL10.AL_CHANNELS);
	int freq = AL10.alGetBufferi(buffer, AL10.AL_FREQUENCY);
	int samples = bytes / (bits / 8);
	
	return (samples / (float) freq) / channels;
}
//############################################################################################################

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//This is where modifiers start.
/**
 * 
 * @param reference
 * @return returns true if the reference is already playing, false if it is not, or doesnt exist.
 */
public static boolean isPlaying(String reference) {
	if (playCheck(reference)){
	return alGetSourcei(sources.get(reference), AL_SOURCE_STATE) == AL_PLAYING;
	}else {
		playCheck(reference);
		return false;
	}
}
//^^^^^^^^^^^^^
/* reserved spot for moving sources
public static void convergence(String reference, speed) {
}
*/
//^^^^^^^^^^^^^
/**
 * this method is used to toggle looping for the reference.
 * @param reference the name you loaded your sound with.
 * @param isLooping do you want your sound to loop?
 */
public static void loop(String reference, boolean isLooping) {
	if (playCheck(reference)){
		if (isLooping) {
			alSourcei(sources.get(reference), AL_LOOPING, AL_TRUE);
			}else {
				alSourcei(sources.get(reference), AL_LOOPING, AL_FALSE);
				}
		}else {
			playCheck(reference);
	}
}
//^^^^^^^^^^^^^
//fix issues here; unable to pause the game with this setup
/**
 * this method is used to pause either a single reference, or an entire category.
 * @param reference either the name of the sound, or the name of the category.
 * @param isCategory a switch to determine if your reference is a single sound, or an entire category.
 */
public static void pause(String reference, boolean isCategory) {
	if (playCheck(reference)){
	if (isCategory) {
		Iterator<Integer> itr = categories.iterator(reference);
		while (itr.hasNext()) {
			alSourcePause(itr.next());
			
		}
  }else {
	alSourcePause(sources.get(reference));
  }
	}else {
		playCheck(reference);
	}
}
//^^^^^^^^^^^^^
//New method! untested!
/**
 * this method is used to continue playing sounds that have been paused.
 * @param reference either the name of the sound, or the name of the category.
 * @param isCategory a switch to determine if your reference is a single sound, or an entire category.
 */
public static void resume(String reference, boolean isCategory) {
	if (playCheck(reference)){
		if(alGetSourcei(sources.get(reference), AL_SOURCE_STATE) == AL_PAUSED) {
			alSourcePlay(sources.get(reference));
		}
		else {
			System.out.println("this source isnt paused!");
			}
		}else {
			playCheck(reference);
			}
	}
//^^^^^^^^^^^^^
//fixes for pause state after this line
/**
 * this method is used to pause every single sound that is playing.
 */
public static void pauseAll() {
	for (String value : soundKeys.values()) {
		if (isPlaying(value)) {
			pauseState.put(value, sources.get(value));
			alSourcePause(sources.get(value));
			}
		}
	}
/**
 * this method is used to resume every single sound that is paused.
 */
public static void resumeAll() {
	for (Integer value : pauseState.values()) {
		alSourcePlay(pauseState.get(value));
		}
	pauseState.clear();
}
//^^^^^^^^^^^^^
/**
 * this method is used to set the volume for a single sound, or an entire category.
 * I need to work on this so that it can work separately from global volume controls. (changing global controls will override individual controls)
 * @param reference either the name of the sound, or the name of the category.
 * @param number 1.0 is 100% volume, hence 0.1 is 10% volume, and 0.01 is 1%
 * @param isCategory a switch to determine if your reference is a single sound, or an entire category.
 *///maybe make another hashmap that's run after the boolean "isCategory" to patch the volume after being adjusted globally?
public static void volume(String reference, float number, boolean isCategory) {
	if (playCheck(reference)){
		if (isCategory) {
			Iterator<Integer> itr = categories.iterator(reference);
			while (itr.hasNext()) {
				alSourcef(itr.next(), AL_GAIN, number);
				}
			}else {
				alSourcef(sources.get(reference), AL_GAIN, number);
				}
		}else {
			playCheck(reference);
			}
	}
//^^^^^^^^^^^^^
/**
 * this method is used to stop a single sound, or an entire category.
 * @param reference either the name of the sound, or the name of the category.
 * @param isCategory a switch to determine if your reference is a single sound, or an entire category.
 */
public static void stop(String reference, boolean isCategory) {
	if (playCheck(reference)){
		if (isCategory) {
			Iterator<Integer> itr = categories.iterator(reference);
			while (itr.hasNext()) {
				alSourceStop(itr.next());
				checkALError();
				}
			}else {
				alSourceStop(sources.get(reference));
				checkALError();
				}
		}else {
			playCheck(reference);
			}
	}
//^^^^^^^^^^^^^
//this hasnt been tested, in theory it should work.
/**
 * this method is used to stop every single sound.
 */
public static void stopAll() {
	for (Integer value : sources.values()) {
	    alSourceStop(value);
	    checkALError();
	}
}
//^^^^^^^^^^^^^
/**
 * this method is used to set properties for your sound... proceed at your own risk. (I have work to do here possibly)
 * @param reference the name you used for your sound
 * @param param the int id of the parameter you want to change
 * @param value the float value of the parameter you want to change
 */
public static void setProperty(String reference, int param, float value) {
	if (playCheck(reference)){
		alSourcef(sources.get(reference), param, value);
	}else {
		playCheck(reference);
	}
}
//^^^^^^^^^^^^^
/**
 * this method sets the starting position of your sound
 * @param reference the name you used for your sound
 * @param i how many seconds from 0 do you want your sound to start from when played
 */
public static void setStart(String reference, int i) {
	IntBuffer secondsOffset = BufferUtils.createIntBuffer(1).put(i);
	secondsOffset.rewind();
	AL11.alSourceiv(sources.get(reference), AL_SEC_OFFSET, secondsOffset);
}
//^^^^^^^^^^^^^
//Master audio levels
/**
 * this method sets the maximum volume for the listener
 * @param x 1.0 is 100% volume, hence 0.1 is 10% volume, and 0.01 is 1%
 */
public static void masterVolume(float x) {
	AL10.alListenerf(AL_GAIN, x);
}
//^^^^^^^^^^^^^

//^^^^^^^^^^^^^
//############################################################################################################
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
/**
 * this method is used to delete the source and buffer for your sound, or all sounds in a category.
 * @param reference either the name of the sound, or the name of the category.
 * @param isCategory a switch to determine if your reference is a single sound, or an entire category.
 */
public static void delete(String reference, boolean isCategory) {
	if (playCheck(reference)){
		if (isCategory) {
			Iterator<Integer> itr = categories.iterator(reference);
			while (itr.hasNext()) {
				//itr.next() gives the value in categories(being the source value)
				alSourceStop(itr.next());
				//hopefully this removes the buffer by checking for the key in sources that contains the value from the iterator
				buffers.remove(buffers.containsKey(sources.containsValue(itr.next())));
				sources.remove(sources.containsValue(itr.next()));
				alDeleteSources(sources.get(itr.next()));
				alDeleteBuffers(buffers.get(itr.next()));
				}
			}else {
				alSourceStop(sources.get(reference));
				categories.removeMapping(reference,(sources.get(reference)));
				buffers.remove(reference);
				sources.remove(reference);
				alDeleteSources(sources.get(reference));
				alDeleteBuffers(buffers.get(reference));
				}
		}else {
			playCheck(reference);
			}
	}
//^^^^^^^^^^^^^
//this hasnt been tested, in theory it should work.
/**
 * this method is used to delete all sounds and buffers.
 */
public static void clearSoft() {
	stopAll();
	for (Integer value : sources.values()) {
		alDeleteSources(value);
		}
	for (Integer value : buffers.values()) {
		alDeleteBuffers(value);
		}
	sources.clear();
	buffers.clear();
	categories.clear();
	soundTime.clear();
	soundKeys.clear();
	instances.clear();
	pauseState.clear();
}
//^^^^^^^^^^^^^
/**
 * this method is used to delete all sounds and buffers, and reset the device and initialized state.
 */
public static void clearHard() {
	stopAll();
	for (Integer value : sources.values()) {
		alDeleteSources(value);
	}
	for (Integer value : buffers.values()) {
		alDeleteBuffers(value);
	}
	alcMakeContextCurrent(NULL);
    alcDestroyContext(ct);
    alcCloseDevice(dev);
	sources.clear();
	buffers.clear();
	categories.clear();
	soundTime.clear();
	soundKeys.clear();
	instances.clear();
	pauseState.clear();
	boolean JBInit = false;
	initialized = false;
}
//############################################################################################################
//!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$%^&*!@#$
}
/*

 You need to be able to: 
 stop the sound while it is currently playing, and this needs to work when that sound is already playing multiple times. (this is mostly done, you can stop sounds based on category, however I havent added the ability to stop the reoccuring clips yet.)
 ability to play sounds from a reference string. (this is complete!)
 add AL effects to the sound such as looping, or fading, and multiple effects at the same time. (looping is done, maybe some others too?)
 have a super simple way of calling audio that is 2 steps or fewer. (complete!)
 you need the ability to play more than a single sound at the same time. (working on this, currently bugged and either crashes when it attempts to clean up unused clips, or pretty much creates a memory leak and you run out of memory...)
 (might be able to reduce it to one call by having the sound check if it is loaded, if not then load.?) - maybe put this method in equilibrium with the 2 part function? -- I decided that although simple, I dont really like this idea and would prefer to plan loading.

*/