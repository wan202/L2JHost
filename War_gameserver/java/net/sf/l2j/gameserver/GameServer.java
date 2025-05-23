package net.sf.l2j.gameserver;

import l2jhost.L2JAngeLInfo;
import l2jhost.DollSystem.DollsData;
import l2jhost.RandomCraft.RandomCraftXML;
import l2jhost.auction.AuctionTable;
import l2jhost.data.IconTable;
import l2jhost.data.custom.CapsuleBox.CapsuleBoxData;
import l2jhost.data.custom.SkillBox.SkillBoxData;
import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.mmocore.SelectorConfig;
import net.sf.l2j.commons.mmocore.SelectorThread;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.util.SysUtil;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.AnnounceOnlinePlayers;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.CrestCache;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.*;
import net.sf.l2j.gameserver.data.sql.*;
import net.sf.l2j.gameserver.data.xml.*;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.boat.*;
import net.sf.l2j.gameserver.model.buffer.SchemeBufferManager;
import net.sf.l2j.gameserver.model.donate.data.DataArmorSet;
import net.sf.l2j.gameserver.model.donate.data.JewelSetData;
import net.sf.l2j.gameserver.model.donate.data.WeaponSetData;
import net.sf.l2j.gameserver.model.donate.handler.DonationHandler;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFManager;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMManager;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMManager;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTManager;
import net.sf.l2j.gameserver.model.entity.events.tournament.TournamentManager;
import net.sf.l2j.gameserver.model.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.GamePacketHandler;
import net.sf.l2j.gameserver.taskmanager.*;
import net.sf.l2j.util.DeadLockDetector;
import net.sf.l2j.util.IPv4Filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.logging.LogManager;

public class GameServer 
{
	private static final CLogger LOGGER = new CLogger(GameServer.class.getName());

	private final SelectorThread<GameClient> _selectorThread;
	private final long _serverStartTimeMillis;

	private static GameServer _gameServer;
	public long serverLoadStart = System.currentTimeMillis();

	public static void main(String[] args) throws Exception 
	{
		_gameServer = new GameServer();
	}

	public GameServer() throws Exception 
	{
		// Create log folder
		new File("./log").mkdir();
		new File("./log/chat").mkdir();
		new File("./log/console").mkdir();
		new File("./log/error").mkdir();
		new File("./log/gmaudit").mkdir();
		new File("./log/item").mkdir();
		new File("./data/crests").mkdirs();

		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File("config/logging.properties"))) 
		{
			LogManager.getLogManager().readConfiguration(is);
		}
			L2JAngeLInfo.showInfo();
			StringUtil.printSection("Config");
			Config.loadGameServer();

			StringUtil.printSection("Poolers");
			ConnectionPool.init();
			ThreadPool.init();

			StringUtil.printSection("IdFactory");
			IdFactory.getInstance();

			StringUtil.printSection("Cache");
			HtmCache.getInstance();
			CrestCache.getInstance();

			StringUtil.printSection("World");
			World.getInstance();
			MapRegionData.getInstance();
			AnnouncementData.getInstance();
			ServerMemoTable.getInstance();

			StringUtil.printSection("Icons");
			SkillsIconsData.getInstance();

			StringUtil.printSection("Skills");
			SkillTable.getInstance();
			SkillTreeData.getInstance();

			StringUtil.printSection("Items");
			ItemData.getInstance();
			SummonItemData.getInstance();
			HennaData.getInstance();
			BuyListManager.getInstance();
			MultisellData.getInstance();
			RecipeData.getInstance();
			ArmorSetData.getInstance();
			FishData.getInstance();
			SpellbookData.getInstance();
			SoulCrystalData.getInstance();
			AugmentationData.getInstance();
			CursedWeaponManager.getInstance();
			SkipData.getInstance();

			StringUtil.printSection("Admins");
			AdminData.getInstance();
			BookmarkTable.getInstance();
			PetitionManager.getInstance();

			StringUtil.printSection("Characters");
			PlayerData.getInstance();
			PlayerInfoTable.getInstance();
			PlayerLevelData.getInstance();
			PartyMatchRoomManager.getInstance();
			RaidPointManager.getInstance();
			HealSpsData.getInstance();

			StringUtil.printSection("Poly Morph initialit.");
			PolyData.getInstance();

			if (Config.ENABLE_BALANCE_ATTACK_TYPE) 
			{
				StringUtil.printSection("Balance Physical initialit.");
				LOGGER.info("Loaded {balance} Physical.");
				BalanceManagerAI.getInstance();
			}

			if (Config.ENABLE_BALANCE_MAGICAL_TYPE)
			{
				StringUtil.printSection("Balance Magical initialit.");
				LOGGER.info("Loaded {balance} Magical.");
				BalanceManagerSkillAI.getInstance();
			}

			StringUtil.printSection("Community server");
			LOGGER.info("Loaded {Community} Bord Manager.");
			CommunityBoard.getInstance();

			StringUtil.printSection("Clans");
			ClanTable.getInstance();

			if (Config.ENABLE_DONATE_INSTANCE)
			{
				StringUtil.printSection("Donate Server Manager");
				DataArmorSet.getInstance();
				WeaponSetData.getInstance();
				JewelSetData.getInstance();
			} 
			else 
			{
				StringUtil.printSection("Donate Server Manager");
				LOGGER.info("Donate Loaded {disable} armor sets.");
				LOGGER.info("Donate Loaded {disable} weapon list.");
				LOGGER.info("Donate Loaded {disable} jewels list.");
			}
			if (Config.ENABLE_STARTUP)
			{
				StartupManager.getInstance();
				LOGGER.info("Newbie System Actived");
			} else
				LOGGER.info("Newbie System Desatived");
			StringUtil.printSection("Geodata & Pathfinding");
			GeoEngine.getInstance();

			StringUtil.printSection("Zones");
			ZoneManager.getInstance();
			StringUtil.printSection("Castles & Clan Halls");
			CastleManager.getInstance();

			StringUtil.printSection("Drop Monsters");
			DropMonstersData.getInstance();
			ClanHallManager.getInstance();
			CrownManager.getInstance();

			StringUtil.printSection("Task Managers");
			AutoFarmTaskManager.getInstance();
			AttackStanceTaskManager.getInstance();
			DecayTaskManager.getInstance();
			GameTimeTaskManager.getInstance();
			ItemsOnGroundTaskManager.getInstance();
			PvpFlagTaskManager.getInstance();
			RandomAnimationTaskManager.getInstance();
			ShadowItemTaskManager.getInstance();
			WaterTaskManager.getInstance();
			DelayedItemsManager.getInstance();
			InstanceManager.getInstance();
			
			StringUtil.printSection("Seven Signs");
			SevenSignsManager.getInstance();
			FestivalOfDarknessManager.getInstance();
			BossEvent.getInstance();

			StringUtil.printSection("Manor Manager");
			ManorAreaData.getInstance();
			CastleManorManager.getInstance();

			StringUtil.printSection("NPCs");
			BufferManager.getInstance();
			SchemeBufferData.getInstance();
			SchemeBufferManager.getInstance();
			NpcData.getInstance();
			WalkerRouteData.getInstance();
			DoorData.getInstance().spawn();
			StaticObjectData.getInstance();
			SpawnManager.getInstance();
			GrandBossManager.getInstance();
			DimensionalRiftManager.getInstance();
			NewbieBuffData.getInstance();
			InstantTeleportData.getInstance();
			TeleportData.getInstance();
			ObserverGroupData.getInstance();

			CastleManager.getInstance().loadArtifacts();

			StringUtil.printSection("Olympiads & Heroes");
			OlympiadGameManager.getInstance();
			Olympiad.getInstance();
			HeroManager.getInstance();

			StringUtil.printSection("Quests & Scripts");
			ScriptData.getInstance();

			if (Config.ALLOW_BOAT) 
			{
				BoatManager.getInstance();
				BoatGiranTalking.load();
				BoatGludinRune.load();
				BoatInnadrilTour.load();
				BoatRunePrimeval.load();
				BoatTalkingGludin.load();
			}

			StringUtil.printSection("Events");
			DerbyTrackManager.getInstance();
			LotteryManager.getInstance();

			CoupleManager.getInstance();

			if (Config.ALLOW_FISH_CHAMPIONSHIP)
				FishingChampionshipManager.getInstance();

			if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
				OfflineTradersTable.restoreOfflineTraders();

			CTFManager.getInstance();
			DMManager.getInstance();
			LMManager.getInstance();
			TvTManager.getInstance();

			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);

			StringUtil.printSection("Spawns");
			SpawnManager.getInstance().spawn();
			DonationHandler donationHandler = new DonationHandler();
			donationHandler.start();
			IconTable.getInstance();

			StringUtil.printSection("CapsuleBox");
			CapsuleBoxData.getInstance();

			StringUtil.printSection("SkillBox");
			SkillBoxData.getInstance();

			StringUtil.printSection("Random Craft");
			RandomCraftXML.getInstance();

			StringUtil.printSection("Dolls");
			DollsData.getInstance();
			
			StringUtil.printSection("Handlers");
			LOGGER.info("Loaded {} Admin handlers.", AdminCommandHandler.getInstance().size());
			LOGGER.info("Loaded {} Chat handlers.", ChatHandler.getInstance().size());
			LOGGER.info("Loaded {} Item handlers.", ItemHandler.getInstance().size());
			LOGGER.info("Loaded {} Skill handlers.", SkillHandler.getInstance().size());
			LOGGER.info("Loaded {} Target handlers.", TargetHandler.getInstance().size());
			LOGGER.info("Loaded {} User handlers.", UserCommandHandler.getInstance().size());
			LOGGER.info("Loaded {} Voiced handlers.", VoicedCommandHandler.getInstance().size());

			StringUtil.printSection("System");
			if (Config.ALLOW_ANNOUNCE_ONLINE_PLAYERS)
				AnnounceOnlinePlayers.getInstance();
			Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());

			if (Config.DEADLOCK_DETECTOR) {
				LOGGER.info("Deadlock detector is enabled. Timer: {}s.", Config.DEADLOCK_CHECK_INTERVAL);

				final DeadLockDetector deadDetectThread = new DeadLockDetector();
				deadDetectThread.setDaemon(true);
				deadDetectThread.start();
			} else
				LOGGER.info("Deadlock detector is disabled.");

			LOGGER.info("Gameserver has started, used memory: {} / {} Mo.", SysUtil.getUsedMemory(),
					SysUtil.getMaxMemory());
			LOGGER.info("Maximum allowed players: {}.", Config.MAXIMUM_ONLINE_USERS);
			LOGGER.info("Server loaded in " + (System.currentTimeMillis() - serverLoadStart) / 1000 + " seconds");
		
		StringUtil.printSection("L2JAngel");
		AuctionTable.getInstance();
		TournamentManager.init();
		
		StringUtil.printSection("Login");
		LoginServerThread.getInstance().start();

		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;

		final GamePacketHandler handler = new GamePacketHandler();
		_selectorThread = new SelectorThread<>(sc, handler, handler, handler, new IPv4Filter());

		_serverStartTimeMillis = System.currentTimeMillis();

		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*")) {
			try {
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			} catch (Exception e) {
				LOGGER.error("The GameServer bind address is invalid, using all available IPs.", e);
			}
		}

		try {
			_selectorThread.openServerSocket(bindAddress, Config.GAMESERVER_PORT);
		} catch (Exception e) {
			LOGGER.error("Failed to open server socket.", e);
			System.exit(1);
		}
		_selectorThread.start();

	}

	public static GameServer getInstance() 
	{
		return _gameServer;
	}

	public SelectorThread<GameClient> getSelectorThread()
	{
		return _selectorThread;
	}

	public long getServerStartTime() 
	{
		return _serverStartTimeMillis;
	}
}