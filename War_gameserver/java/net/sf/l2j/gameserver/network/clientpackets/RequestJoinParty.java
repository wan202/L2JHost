package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.LootRule;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinParty extends L2GameClientPacket {
	private String _targetName;
	private int _lootRuleId;

	private int MAX_PARTY_HEALER_BISHOP = Config.MAX_PARTY_HEALER_BISHOP;
	private int MAX_PARTY_ORC_OVER = Config.MAX_PARTY_ORC_OVER;
	private int MAX_PARTY_DUELISTA = Config.MAX_PARTY_DUELISTA;
	private int MAX_PARTY_WARLORD = Config.MAX_PARTY_WARLORD;

	@Override
	protected void readImpl() {
		_targetName = readS();
		_lootRuleId = readD();
	}

	@Override
	protected void runImpl() {
		final Player requestor = getClient().getPlayer();
		if (requestor == null)
			return;

		final Player target = World.getInstance().getPlayer(_targetName);
		if (target == null) {
			requestor.sendPacket(SystemMessageId.FIRST_SELECT_USER_TO_INVITE_TO_PARTY);
			return;
		}

		if (target.getBlockList().isBlockingAll()) {
			requestor.sendPacket(
					SystemMessage.getSystemMessage(SystemMessageId.S1_BLOCKED_EVERYTHING).addCharName(target));
			return;
		}

		if (target.getBlockList().isInBlockList(requestor)) {
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST)
					.addCharName(target));
			return;
		}

		if (target.equals(requestor) || target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped()
				|| !target.getAppearance().isVisible()) {
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		if (target.isInParty()) {
			requestor.sendPacket(
					SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addCharName(target));
			return;
		}

		if (CTFEvent.isPlayerParticipant(target.getObjectId()) || DMEvent.isPlayerParticipant(target.getObjectId())
				|| LMEvent.isPlayerParticipant(target.getObjectId())
				|| TvTEvent.isPlayerParticipant(target.getObjectId())) {
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		if (target.getClient() == null || target.getClient().isDetached()) {
			requestor.sendMessage("The player you tried to invite is in offline mode.");
			return;
		}

		if (target.isInJail() || requestor.isInJail()) {
			requestor.sendMessage("The player you tried to invite is currently jailed.");
			return;
		}

		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
			return;

		if (requestor.isProcessingRequest()) {
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if (target.isProcessingRequest()) {
			requestor.sendPacket(
					SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addCharName(target));
			return;
		}
		if (target.isInTournament() || requestor.isInTournament()
				|| requestor.isInTournament() && target.isInTournament() && requestor.getTeam() != target.getTeam()) {
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		final Party party = requestor.getParty();
		if (party != null) {
			if (!party.isLeader(requestor)) {
				requestor.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
				return;
			}

			if (party.getMembersCount() >= 9) {
				requestor.sendPacket(SystemMessageId.PARTY_FULL);
				return;
			}
			if (party.getMembers().stream().filter(k -> k.getClassId() == (ClassId.CARDINAL))
					.count() >= MAX_PARTY_HEALER_BISHOP) {
				requestor.sendMessage("Maximum " + MAX_PARTY_HEALER_BISHOP + " healers are allowed in party !");
				return;
			}

			if (party.getMembers().stream().filter(k -> k.getClassId() == (ClassId.OVERLORD))
					.count() >= MAX_PARTY_ORC_OVER) {
				requestor.sendMessage("Maximum " + MAX_PARTY_ORC_OVER + " Overlord are allowed in party !");
				return;
			}
			if (party.getMembers().stream().filter(k -> k.getClassId() == (ClassId.DUELIST))
					.count() >= MAX_PARTY_DUELISTA) {
				requestor.sendMessage("Maximum " + MAX_PARTY_DUELISTA + " Duelista are allowed in party !");
				return;
			}
			if (party.getMembers().stream().filter(k -> k.getClassId() == (ClassId.WARLORD))
					.count() >= MAX_PARTY_WARLORD) {
				requestor.sendMessage("Maximum " + MAX_PARTY_WARLORD + "Duelista are allowed in party !");
				return;
			}

			if (party.getPendingInvitation() && !party.isInvitationRequestExpired()) {
				requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
				return;
			}

			party.setPendingInvitation(true);
		} else
			requestor.setLootRule(LootRule.VALUES[_lootRuleId]);

		requestor.onTransactionRequest(target);
		requestor.sendPacket(
				SystemMessage.getSystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addCharName(target));

		target.sendPacket(
				new AskJoinParty(requestor.getName(), (party != null) ? party.getLootRule().ordinal() : _lootRuleId));
	}
}