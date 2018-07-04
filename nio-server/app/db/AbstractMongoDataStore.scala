package db

import db.MongoOpsDataStore.MongoDataStore
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api.indexes.Index
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractMongoDataStore[T](mongoApi: ReactiveMongoApi)(
    implicit val format: OFormat[T],
    executionContext: ExecutionContext) {

  protected def collectionName(tenant: String): String

  protected def indices: Seq[Index]

  protected def storedCollection(tenant: String): Future[JSONCollection] =
    mongoApi.database.map(_.collection(collectionName(tenant)))

  def init(tenant: String): Future[Unit] = {
    storedCollection(tenant).flatMap { col =>
      for {
        _ <- col.drop(failIfNotFound = false)
        _ <- col.create()
      } yield ()
    }
  }

  def ensureIndices(tenant: String): Future[Unit] =
    for {
      db <- mongoApi.database
      cName = collectionName(tenant)
      foundName <- db.collectionNames.map(names => names.contains(cName))
      _ <- if (foundName) {
        Logger.info(s"Ensuring indices for $cName")
        val col = db.collection[JSONCollection](cName)
        Future.sequence(indices.map(i => col.indexesManager.ensure(i)))
      } else {
        Future.successful(Seq.empty[Boolean])
      }
    } yield {
      ()
    }

  def insertOne(tenant: String, objToInsert: T): Future[Boolean] = {
    storedCollection(tenant).flatMap(
      _.insertOne[T](objToInsert)
    )
  }

  def updateOne(tenant: String, id: String, objToUpdate: T): Future[Boolean] = {
    storedCollection(tenant).flatMap {
      _.updateOne(id, objToUpdate)
    }
  }

  def updateOneByQuery(tenant: String,
                       query: JsObject,
                       objToUpdate: T): Future[Boolean] = {
    storedCollection(tenant).flatMap {
      _.updateOneByQuery(query, objToUpdate)
    }
  }

  def findOneById(tenant: String, id: String): Future[Option[T]] = {
    storedCollection(tenant).flatMap {
      _.findOneById(id)
    }
  }

  def findOneByQuery(tenant: String, query: JsObject): Future[Option[T]] = {
    storedCollection(tenant).flatMap {
      _.findOneByQuery(query)
    }
  }

  def findMany(tenant: String): Future[Seq[T]] = {
    storedCollection(tenant).flatMap {
      _.findMany()
    }
  }

  def findManyByQuery(tenant: String, query: JsObject): Future[Seq[T]] = {
    storedCollection(tenant).flatMap {
      _.findManyByQuery(query)
    }
  }

  def findManyByQueryPaginateCount(tenant: String,
                                   query: JsObject,
                                   sort: JsObject = Json.obj("_id" -> 1),
                                   page: Int,
                                   pageSize: Int): Future[(Seq[T], Int)] = {
    storedCollection(tenant).flatMap {
      _.findManyByQueryPaginateCount(tenant, query, sort, page, pageSize)
    }
  }

  def findManyByQueryPaginate(tenant: String,
                              query: JsObject,
                              sort: JsObject = Json.obj("_id" -> -1),
                              page: Int,
                              pageSize: Int): Future[Seq[T]] = {
    storedCollection(tenant).flatMap {
      _.findManyByQueryPaginate(tenant, query, sort, page, pageSize)
    }
  }

  private def find(tenant: String, query: JsObject): Future[Seq[T]] = {
    storedCollection(tenant).flatMap {
      _.findManyByQuery(query)
    }
  }

  def deleteOneById(tenant: String, id: String): Future[Boolean] = {
    storedCollection(tenant).flatMap {
      _.deleteOneById(id)
    }
  }

  def deleteByQuery(tenant: String, query: JsObject): Future[Boolean] = {
    storedCollection(tenant).flatMap {
      _.deleteByQuery(query)
    }
  }

}
