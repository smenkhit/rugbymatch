package com.paulienvanalst.rugbymatch.game

import com.paulienvanalst.rugbymatch.Application
import com.paulienvanalst.rugbymatch.analytics.GameReporter
import com.paulienvanalst.rugbymatch.analytics.MailingService
import com.paulienvanalst.rugbymatch.events.FinishGame
import com.paulienvanalst.rugbymatch.events.HalfTime
import com.paulienvanalst.rugbymatch.events.ScoringEvent
import com.paulienvanalst.rugbymatch.events.StartGame
import com.paulienvanalst.rugbymatch.team.Team
import com.paulienvanalst.rugbymatch.team.TeamName
import junit.framework.Assert.assertTrue
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = arrayOf(Application::class))
class EventFlowTest {

    @Autowired
    private lateinit var scoringBoard: ScoringBoard

    @Autowired
    private lateinit var gameReporter: GameReporter

    @Autowired
    private  lateinit var  mailingService: MailingService

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @BeforeEach
    fun `start a game` () {
        eventPublisher.publishEvent(StartGame(this, TeamName.RC_TOULON, TeamName.WASPS))
    }

    @Test
    fun `Start of a game instantiates the scoring board and the game report`() {
        assertThat(scoringBoard, notNullValue())
        assertThat(scoringBoard.currentScore(), `is`(GameScore(TeamName.RC_TOULON to 0, TeamName.WASPS to 0)))

        assertThat(gameReporter, notNullValue())
    }

    @Test
    fun `when a try is scored the scoring board is updated` () {
        eventPublisher.publishEvent(ScoringEvent(this, Type.TRY, TeamName.RC_TOULON))

        assertThat(scoringBoard.currentScore(), `is`(GameScore(TeamName.RC_TOULON to 5, TeamName.WASPS to 0)))
    }

    @Test
    fun `Game report fixes the score at half time and end of game - It is generated at end of game`() {
        eventPublisher.publishEvent(ScoringEvent(this, Type.TRY, TeamName.RC_TOULON))

        eventPublisher.publishEvent(HalfTime(this))

        eventPublisher.publishEvent(ScoringEvent(this, Type.TRY, TeamName.WASPS))

        eventPublisher.publishEvent(FinishGame(this))


        assertThat(mailingService.mailToSend, containsString("half-time score was: " + GameScore(TeamName.RC_TOULON to 5, TeamName.WASPS to 0).toString()))
        assertThat(mailingService.mailToSend,  containsString( "ended with the score of " + GameScore(TeamName.RC_TOULON to 5, TeamName.WASPS to 5).toString()))

    }
}