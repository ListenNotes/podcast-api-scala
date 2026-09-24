package podcast.api.scala.demo

import com.listennotes.podcast_api.Client
import com.listennotes.podcast_api.exception.PermissionDeniedException
import org.junit.jupiter.api._
import org.junit.jupiter.api.Assertions._
import podcast.api.testing.Support
import scala.jdk.CollectionConverters._

class ExamplesTest {
  @TestFactory
  def allMethods(): java.util.List[DynamicTest] = Support.operations().asScala.map { op =>
    DynamicTest.dynamicTest(op.getString("func"), () => {
      val server = new Support()
      try {
        val client = new Client("scala-test", server.baseUrl())
        val parameters = Support.parameters(op)
        val before = new java.util.HashMap[String, String](parameters)
        val response = GeneratedExamples.call(client, op.getString("operationId"), parameters)
        assertTrue(response.toJSON().getBoolean("ok"))
        assertEquals(200, response.getStatusCode())
        assertEquals(12, response.getUsage().intValue())
        assertEquals(300, response.getFreeQuota().intValue())
        val request = server.take()
        assertEquals("scala-test", request.key())
        Support.verify(op, parameters, request)
        assertEquals(before, parameters)
      } finally server.close()
    })
  }.asJava

  @Test
  def nestedPathsEmptyFieldsAndClientIsolation(): Unit = {
    val first = new Support()
    val second = new Support()
    try {
      val one = new Client("first", first.baseUrl())
      val two = new Client("second", second.baseUrl())
      one.updatePlaylistItemNotes(Map("id" -> "a/b ?#é", "item_id" -> "23", "notes" -> "").asJava)
      val request = first.take()
      assertEquals("/api/v2/playlists/a%2Fb%20%3F%23%C3%A9/items/23", request.uri().getRawPath())
      assertEquals(Map("notes" -> "").asJava, Support.decode(request.body()))
      assertEquals("first", request.key())
      two.updatePlaylist(Map("id" -> "abc", "description" -> "").asJava)
      val other = second.take()
      assertEquals("second", other.key())
      assertEquals(Map("description" -> "").asJava, Support.decode(other.body()))
      one.search(Map("q" -> "café + & /").asJava)
      assertEquals(Map("q" -> "café + & /").asJava, Support.decode(first.take().uri().getRawQuery()))
      first.status = 403
      val error = assertThrows(classOf[PermissionDeniedException], () => one.search(Map.empty[String, String].asJava))
      assertEquals(403, error.getResponse().getStatusCode())
      assertTrue(error.getResponse().toJSON().getBoolean("ok"))
    } finally { first.close(); second.close() }
  }
}

@Tag("integration")
class MockIntegrationTest {
  // Never read credentials or a destination from the environment.
  @TestFactory
  def allMethods(): java.util.List[DynamicTest] = Support.operations().asScala.map { op =>
    DynamicTest.dynamicTest(op.getString("func"), () => {
      val response = GeneratedExamples.call(new Client(), op.getString("operationId"), Support.parameters(op))
      assertTrue(Set(200, 201).contains(response.getStatusCode()))
      assertFalse(response.toJSON().isEmpty())
    })
  }.asJava

  @Test
  def clearFieldsAndAddPodcast(): Unit = {
    val client = new Client()
    assertFalse(client.updatePlaylistItemNotes(Map("id" -> "m1pe7z60bsw", "item_id" -> "23", "notes" -> "").asJava).toJSON().isEmpty())
    assertFalse(client.updatePlaylist(Map("id" -> "m1pe7z60bsw", "description" -> "").asJava).toJSON().isEmpty())
    assertFalse(client.addPlaylistItem(Map("id" -> "m1pe7z60bsw", "podcast_id" -> "4d3fe717742d4963a85562e9f84d8c79").asJava).toJSON().isEmpty())
  }
}
