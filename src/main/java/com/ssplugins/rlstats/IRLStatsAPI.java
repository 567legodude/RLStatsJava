package com.ssplugins.rlstats;

import com.mashape.unirest.http.JsonNode;
import com.ssplugins.rlstats.entities.*;
import com.ssplugins.rlstats.util.PlayerRequest;
import com.ssplugins.rlstats.util.Query;
import com.ssplugins.rlstats.util.RequestQueue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class IRLStatsAPI implements RLStatsAPI {
	
	private final ExecutorService tasks = Executors.newCachedThreadPool();
	
	private RequestQueue queue = new RequestQueue();
	
	private String key = null;
	private String apiVersion = "v1";
	private boolean shutdown = false;
	
	IRLStatsAPI() {}
	
	private void basicCheck() {
		if (shutdown)
			throw new IllegalStateException("Threads have been shutdown. Create a new instance to make more requests");
		if (key == null || key.isEmpty()) throw new IllegalStateException("No API key was provided.");
	}
	
	private List<Player> jsonArrayToList(JsonNode node) {
		List<Player> list = new ArrayList<>();
		node.getArray().forEach(o -> {
			JSONObject object = (JSONObject) o;
			list.add(new Player(object));
		});
		return list;
	}
	
	@Override
	public void shutdownThreads() {
		shutdown = true;
		queue.shutdown();
		tasks.shutdownNow();
	}
	
	@Override
	public void setAuthKey(String key) {
		this.key = key;
	}
	
	@Override
	public String getAuthKey() {
		return key;
	}
	
	@Override
	public void setAPIVersion(String version) {
		this.apiVersion = version;
	}
	
	@Override
	public String getAPIVersion() {
		return apiVersion;
	}
	
	@Override
	public Future<List<PlatformInfo>> getPlatforms() {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/data/platforms", null);
			try {
				JsonNode node = response.get();
				List<PlatformInfo> list = new ArrayList<>();
				node.getArray().forEach(o -> {
					JSONObject object = (JSONObject) o;
					list.add(new PlatformInfo(object));
				});
				return list;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public List<PlatformInfo> getPlatformsBlocking() {
		try {
			return getPlatforms().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<List<Tier>> getTiers() {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/data/tiers", null);
			return getTiers(response);
		});
	}
	
	@Override
	public Future<List<Tier>> getTiers(int season) {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/data/tiers/" + season, null);
			return getTiers(response);
		});
	}
	
	private List<Tier> getTiers(Future<JsonNode> response) {
		try {
			JsonNode node = response.get();
			List<Tier> list = new ArrayList<>();
			node.getArray().forEach(o -> {
				JSONObject object = (JSONObject) o;
				list.add(new Tier(object));
			});
			return list;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Tier> getTiersBlocking() {
		try {
			return getTiers().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Tier> getTiersBlocking(int season) {
		try {
			return getTiers(season).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<List<Season>> getSeasons() {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/data/seasons", null);
			try {
				JsonNode node = response.get();
				List<Season> list = new ArrayList<>();
				node.getArray().forEach(o -> {
					JSONObject object = (JSONObject) o;
					list.add(new Season(object));
				});
				return list;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public List<Season> getSeasonsBlocking() {
		try {
			return getSeasons().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<List<Playlist>> getPlaylistInfo() {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/data/playlists", null);
			try {
				JsonNode node = response.get();
				List<Playlist> list = new ArrayList<>();
				node.getArray().forEach(o -> {
					JSONObject object = (JSONObject) o;
					list.add(new Playlist(object));
				});
				return list;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public List<Playlist> getPlaylistInfoBlocking() {
		try {
			return getPlaylistInfo().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<Player> getPlayer(String id, int platform) {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/player", Query.create("unique_id", id).add("platform_id", String.valueOf(platform)));
			try {
				JsonNode node = response.get();
				return new Player(node.getObject());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return null;
		});
	}
	
	@Override
	public Future<Player> getPlayer(String id, Platform platform) {
		return getPlayer(id, platform.getId());
	}
	
	@Override
	public Player getPlayerBlocking(String id, int platform) {
		try {
			return getPlayer(id, platform).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Player getPlayerBlocking(String id, Platform platform) {
		return getPlayerBlocking(id, platform.getId());
	}
	
	@Override
	public Future<List<Player>> getPlayers(Collection<PlayerRequest> collection) {
		if (collection.size() > 10) throw new IllegalArgumentException("Cannot have more than 10 players requested.");
		basicCheck();
		return tasks.submit(() -> {
			JSONArray array = new JSONArray();
			collection.forEach(playerRequest -> array.put(playerRequest.toJSONObject()));
			
			Future<JsonNode> response = queue.post(key, apiVersion, "/player/batch", null, array.toString());
			try {
				JsonNode node = response.get();
				System.out.println(node.getArray().toString());
				return jsonArrayToList(node);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public Future<List<Player>> getPlayers(PlayerRequest[] requests) {
		return getPlayers(Arrays.asList(requests));
	}
	
	@Override
	public List<Player> getPlayersBlocking(Collection<PlayerRequest> collection) {
		try {
			return getPlayers(collection).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public List<Player> getPlayersBlocking(PlayerRequest[] requests) {
		try {
			return getPlayers(requests).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<SearchResultPage> searchPlayers(String displayName, int page) {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/search/players", Query.create("display_name", displayName).add("page", String.valueOf(page)));
			try {
				JsonNode node = response.get();
				return new SearchResultPage(node.getObject());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return null;
		});
	}
	
	@Override
	public Future<SearchResultPage> searchPlayers(String displayName) {
		return searchPlayers(displayName, 0);
	}
	
	@Override
	public SearchResultPage searchPlayersBlocking(String displayName, int page) {
		try {
			return searchPlayers(displayName, page).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public SearchResultPage searchPlayersBlocking(String displayName) {
		try {
			return searchPlayers(displayName).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Future<List<Player>> getRankedLeaderboard(int playlistId) {
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/ranked", Query.create("playlist_id", String.valueOf(playlistId)));
			try {
				JsonNode node = response.get();
				return jsonArrayToList(node);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public List<Player> getRankedLeaderboardBlocking(int playlistId) {
		try {
			return getRankedLeaderboard(playlistId).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	@Override
	public Future<List<Player>> getStatLeaderboard(Stat stat) {
		if (stat == null) throw new IllegalArgumentException("Stat parameter is null.");
		basicCheck();
		return tasks.submit(() -> {
			Future<JsonNode> response = queue.get(key, apiVersion, "/leaderboard/stat", Query.create("type", stat.getQueryName()));
			try {
				JsonNode node = response.get();
				return jsonArrayToList(node);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return new ArrayList<>();
		});
	}
	
	@Override
	public List<Player> getStatLeaderboardBlocking(Stat stat) {
		try {
			return getStatLeaderboard(stat).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
}
