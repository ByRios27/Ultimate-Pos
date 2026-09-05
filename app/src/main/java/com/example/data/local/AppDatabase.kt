package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        User::class,
        Draw::class,
        PriceConfig::class,
        Customer::class,
        Sale::class,
        SaleItem::class,
        AuditLog::class,
        DrawResult::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao
    abstract fun priceDao(): PriceDao
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun auditDao(): AuditDao
    abstract fun drawResultDao(): DrawResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: AppDatabase? = null
                val callback = object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            (instance ?: INSTANCE)?.let { database ->
                                ensureInitialData(database)
                            }
                        }
                    }
                }
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sorteos_pos_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(callback)
                .build()
                INSTANCE = instance
                instance!!
            }
        }

        suspend fun ensureInitialData(db: AppDatabase) {
            if (db.drawDao().getDrawCount() == 0 || db.userDao().getUserCount() == 0) {
                populateInitialData(db)
            } else {
                if (db.saleDao().getTotalSalesCount() == 0) {
                    populateInitialSales(db)
                }
                if (db.drawResultDao().getResultForDraw("draw_nic_10pm") == null) {
                    populateInitialResults(db)
                }
            }
        }

        private suspend fun populateInitialResults(db: AppDatabase) {
            val demoResults = listOf(
                DrawResult(
                    id = "res_prim_6pm",
                    drawId = "draw_prim_6pm",
                    drawName = "La Primera",
                    firstPrize = "67",
                    secondPrize = "97",
                    thirdPrize = "68",
                    drawDate = "31/08/2026"
                ),
                DrawResult(
                    id = "res_monazo_530pm",
                    drawId = "draw_monazo_530pm",
                    drawName = "Monazo",
                    firstPrize = "82",
                    secondPrize = "45",
                    thirdPrize = "55",
                    drawDate = "31/08/2026"
                ),
                DrawResult(
                    id = "res_ang_9am",
                    drawId = "draw_ang_9am",
                    drawName = "Anguila",
                    firstPrize = "94",
                    secondPrize = "04",
                    thirdPrize = "03",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_nic_10pm",
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    firstPrize = "44",
                    secondPrize = "16",
                    thirdPrize = "69",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_hon_10pm",
                    drawId = "draw_hon_10pm",
                    drawName = "Honduras",
                    firstPrize = "40",
                    secondPrize = "91",
                    thirdPrize = "56",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_ny_930pm",
                    drawId = "draw_ny_930pm",
                    drawName = "New York",
                    firstPrize = "42",
                    secondPrize = "62",
                    thirdPrize = "39",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_flo_850pm",
                    drawId = "draw_flo_850pm",
                    drawName = "Florida",
                    firstPrize = "04",
                    secondPrize = "35",
                    thirdPrize = "43",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_tica_830pm",
                    drawId = "draw_tica_830pm",
                    drawName = "Tica",
                    firstPrize = "35",
                    secondPrize = "96",
                    thirdPrize = "60",
                    drawDate = "29/08/2026"
                )
            )
            db.drawResultDao().insertResults(demoResults)
        }

        private suspend fun populateInitialSales(db: AppDatabase) {
            val sellerUser = db.userDao().getUserByUsername("vendedor") ?: db.userDao().getUserByUsername("admin")
            val sellerId = sellerUser?.id ?: "user_seller_01"
            val sellerName = sellerUser?.name ?: "Carlos Vendedor"
            val cust1 = db.customerDao().getCustomerById("cust_01")
            val cust2 = db.customerDao().getCustomerById("cust_02")

            // Sales for 6:00 PM La Primera (154 pieces total)
            val primSale1 = Sale(
                id = "sale_prim_01",
                ticketNumber = "#000140",
                customerId = "cust_phone_1",
                customerName = "62337414",
                userId = sellerId,
                userName = sellerName,
                subtotal = 0.60,
                commission = 0.09,
                total = 0.60,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 1800000 // 5:57 pm
            )
            val primItems1 = listOf(
                SaleItem(id = "item_prim_01", saleId = primSale1.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "30", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60)
            )
            db.saleDao().insertSale(primSale1)
            db.saleDao().insertSaleItems(primItems1)

            val primSale2 = Sale(
                id = "sale_prim_02",
                ticketNumber = "#000141",
                customerId = "cust_phone_2",
                customerName = "69532579",
                userId = sellerId,
                userName = sellerName,
                subtotal = 0.60,
                commission = 0.09,
                total = 0.60,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 2400000 // 5:46 pm
            )
            val primItems2 = listOf(
                SaleItem(id = "item_prim_02", saleId = primSale2.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "30", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60)
            )
            db.saleDao().insertSale(primSale2)
            db.saleDao().insertSaleItems(primItems2)

            val primSale3 = Sale(
                id = "sale_prim_03",
                ticketNumber = "#000142",
                customerId = "cust_phone_3",
                customerName = "66424514",
                userId = sellerId,
                userName = sellerName,
                subtotal = 1.00,
                commission = 0.15,
                total = 1.00,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3000000 // 5:40 pm
            )
            val primItems3 = listOf(
                SaleItem(id = "item_prim_03", saleId = primSale3.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "30", digits = 2, quantity = 1.0, unitPrice = 1.00, total = 1.00)
            )
            db.saleDao().insertSale(primSale3)
            db.saleDao().insertSaleItems(primItems3)

            val primBulkSale = Sale(
                id = "sale_prim_bulk",
                ticketNumber = "#000143",
                customerId = cust1?.id ?: "cust_01",
                customerName = "Juan Pérez",
                userId = sellerId,
                userName = sellerName,
                subtotal = 45.00,
                commission = 6.75,
                total = 45.00,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000
            )
            val primBulkItems = listOf(
                SaleItem(id = "item_prim_30_rest", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "30", digits = 2, quantity = 19.0, unitPrice = 0.20, total = 3.80),
                SaleItem(id = "item_prim_22", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "22", digits = 2, quantity = 20.0, unitPrice = 0.20, total = 4.00),
                SaleItem(id = "item_prim_09", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "09", digits = 2, quantity = 10.0, unitPrice = 0.20, total = 2.00),
                SaleItem(id = "item_prim_70", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "70", digits = 2, quantity = 7.0, unitPrice = 0.20, total = 1.40),
                SaleItem(id = "item_prim_72", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "72", digits = 2, quantity = 7.0, unitPrice = 0.20, total = 1.40),
                SaleItem(id = "item_prim_77", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "77", digits = 2, quantity = 6.0, unitPrice = 0.20, total = 1.20),
                SaleItem(id = "item_prim_24", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "24", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_prim_28", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "28", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_prim_55", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "55", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_prim_65", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "65", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_prim_03", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "03", digits = 2, quantity = 4.0, unitPrice = 0.20, total = 0.80),
                SaleItem(id = "item_prim_00", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "00", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_prim_01", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "01", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_18", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "18", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_20", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "20", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_23", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "23", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_26", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "26", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_80", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "80", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_92", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "92", digits = 2, quantity = 3.0, unitPrice = 0.20, total = 0.60),
                SaleItem(id = "item_prim_25", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "25", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_32", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "32", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_35", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "35", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_37", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "37", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_52", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "52", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_58", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "58", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_85", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "85", digits = 2, quantity = 2.0, unitPrice = 0.20, total = 0.40),
                SaleItem(id = "item_prim_27", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "27", digits = 2, quantity = 1.0, unitPrice = 0.20, total = 0.20),
                SaleItem(id = "item_prim_53", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "53", digits = 2, quantity = 1.0, unitPrice = 0.20, total = 0.20),
                SaleItem(id = "item_prim_62", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "62", digits = 2, quantity = 1.0, unitPrice = 0.20, total = 0.20),
                SaleItem(id = "item_prim_73", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "CHANCE", number = "73", digits = 2, quantity = 1.0, unitPrice = 0.20, total = 0.20),
                // Combinaciones (Pales)
                SaleItem(id = "item_prim_p01", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "PALE", number = "03-85", digits = 2, quantity = 1.0, unitPrice = 1.00, total = 1.00),
                SaleItem(id = "item_prim_p02", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "PALE", number = "18-30", digits = 2, quantity = 1.0, unitPrice = 1.00, total = 1.00),
                SaleItem(id = "item_prim_p03", saleId = primBulkSale.id, drawId = "draw_prim_6pm", drawName = "La Primera", drawIcon = "🇩🇴", drawTime = "6:00 PM", modality = "PALE", number = "30-22", digits = 2, quantity = 1.0, unitPrice = 1.00, total = 1.00)
            )
            db.saleDao().insertSale(primBulkSale)
            db.saleDao().insertSaleItems(primBulkItems)

            // Sales for 5:30 PM Monazo (132 pieces)
            val monazoSale = Sale(
                id = "sale_monazo_01",
                ticketNumber = "#000144",
                customerId = cust2?.id ?: "cust_02",
                customerName = "María Rodríguez",
                userId = sellerId,
                userName = sellerName,
                subtotal = 26.40,
                commission = 3.96,
                total = 26.40,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 4000000
            )
            val monazoItems = listOf(
                SaleItem(id = "item_mon_82", saleId = monazoSale.id, drawId = "draw_monazo_530pm", drawName = "Monazo", drawIcon = "🇨🇷", drawTime = "5:30 PM", modality = "CHANCE", number = "82", digits = 2, quantity = 45.0, unitPrice = 0.20, total = 9.00),
                SaleItem(id = "item_mon_45", saleId = monazoSale.id, drawId = "draw_monazo_530pm", drawName = "Monazo", drawIcon = "🇨🇷", drawTime = "5:30 PM", modality = "CHANCE", number = "45", digits = 2, quantity = 35.0, unitPrice = 0.20, total = 7.00),
                SaleItem(id = "item_mon_55", saleId = monazoSale.id, drawId = "draw_monazo_530pm", drawName = "Monazo", drawIcon = "🇨🇷", drawTime = "5:30 PM", modality = "CHANCE", number = "55", digits = 2, quantity = 52.0, unitPrice = 0.20, total = 10.40)
            )
            db.saleDao().insertSale(monazoSale)
            db.saleDao().insertSaleItems(monazoItems)

            val sale1 = Sale(
                id = "sale_demo_01",
                ticketNumber = "#000124",
                customerId = cust1?.id ?: "cust_01",
                customerName = "Evelio",
                userId = sellerId,
                userName = sellerName,
                subtotal = 0.20,
                commission = 0.03,
                total = 0.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 5
            )
            val items1 = listOf(
                SaleItem(
                    id = "item_demo_01",
                    saleId = sale1.id,
                    drawId = "draw_ang_9am",
                    drawName = "Anguila",
                    drawIcon = "🇦🇮",
                    drawTime = "9:00 AM",
                    modality = "CHANCE",
                    number = "21",
                    digits = 2,
                    quantity = 1.0,
                    unitPrice = 0.20,
                    total = 0.20
                )
            )
            db.saleDao().insertSale(sale1)
            db.saleDao().insertSaleItems(items1)

            val sale2 = Sale(
                id = "sale_demo_02",
                ticketNumber = "#000125",
                customerId = cust2?.id ?: "cust_02",
                customerName = "María Rodríguez",
                userId = sellerId,
                userName = sellerName,
                subtotal = 33.40,
                commission = 5.01,
                total = 33.40,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 4
            )
            val items2 = listOf(
                SaleItem(
                    id = "item_demo_03",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "44",
                    digits = 2,
                    quantity = 50.0,
                    unitPrice = 0.20,
                    total = 10.00
                ),
                SaleItem(
                    id = "item_demo_04",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "16",
                    digits = 2,
                    quantity = 60.0,
                    unitPrice = 0.20,
                    total = 12.00
                ),
                SaleItem(
                    id = "item_demo_05",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "69",
                    digits = 2,
                    quantity = 57.0,
                    unitPrice = 0.20,
                    total = 11.40
                )
            )
            db.saleDao().insertSale(sale2)
            db.saleDao().insertSaleItems(items2)

            val sale3 = Sale(
                id = "sale_demo_03",
                ticketNumber = "#000126",
                customerId = cust1?.id ?: "cust_01",
                customerName = "Roberto Gómez",
                userId = sellerId,
                userName = sellerName,
                subtotal = 28.20,
                commission = 4.23,
                total = 28.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 3
            )
            val items3 = listOf(
                SaleItem(
                    id = "item_demo_06",
                    saleId = sale3.id,
                    drawId = "draw_hon_10pm",
                    drawName = "Honduras",
                    drawIcon = "🇭🇳",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "40",
                    digits = 2,
                    quantity = 5.0,
                    unitPrice = 0.10,
                    total = 28.20
                )
            )
            db.saleDao().insertSale(sale3)
            db.saleDao().insertSaleItems(items3)

            val sale4 = Sale(
                id = "sale_demo_04",
                ticketNumber = "#000127",
                customerId = cust2?.id ?: "cust_02",
                customerName = "Laura Silva",
                userId = sellerId,
                userName = sellerName,
                subtotal = 15.80,
                commission = 2.37,
                total = 15.80,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 2
            )
            val items4 = listOf(
                SaleItem(
                    id = "item_demo_07",
                    saleId = sale4.id,
                    drawId = "draw_ny_930pm",
                    drawName = "New York",
                    drawIcon = "🗽",
                    drawTime = "9:30 PM",
                    modality = "CHANCE",
                    number = "42",
                    digits = 2,
                    quantity = 3.0,
                    unitPrice = 0.10,
                    total = 15.80
                )
            )
            db.saleDao().insertSale(sale4)
            db.saleDao().insertSaleItems(items4)

            val sale5 = Sale(
                id = "sale_demo_05",
                ticketNumber = "#000128",
                customerId = cust1?.id ?: "cust_01",
                customerName = "Fernando Castro",
                userId = sellerId,
                userName = sellerName,
                subtotal = 42.20,
                commission = 6.33,
                total = 42.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000
            )
            val items5 = listOf(
                SaleItem(
                    id = "item_demo_08",
                    saleId = sale5.id,
                    drawId = "draw_tica_830pm",
                    drawName = "Tica",
                    drawIcon = "🇨🇷",
                    drawTime = "8:30 PM",
                    modality = "CHANCE",
                    number = "35",
                    digits = 2,
                    quantity = 8.0,
                    unitPrice = 0.20,
                    total = 42.20
                )
            )
            db.saleDao().insertSale(sale5)
            db.saleDao().insertSaleItems(items5)

            // Additional sales for Nica 10:00 PM to showcase pagination (>5 tickets per draw)
            val nicaExtraSales = listOf(
                Triple("#000129", "Carlos Gómez", listOf(SaleItem(id = "item_demo_09", saleId = "sale_demo_06", drawId = "draw_nic_10pm", drawName = "Nica", drawIcon = "🇳🇮", drawTime = "10:00 PM", modality = "CHANCE", number = "12", digits = 2, quantity = 10.0, unitPrice = 0.20, total = 2.00))),
                Triple("#000130", "Ana Morales", listOf(SaleItem(id = "item_demo_10", saleId = "sale_demo_07", drawId = "draw_nic_10pm", drawName = "Nica", drawIcon = "🇳🇮", drawTime = "10:00 PM", modality = "CHANCE", number = "88", digits = 2, quantity = 15.0, unitPrice = 0.20, total = 3.00))),
                Triple("#000131", "Juan Pérez", listOf(SaleItem(id = "item_demo_11", saleId = "sale_demo_08", drawId = "draw_nic_10pm", drawName = "Nica", drawIcon = "🇳🇮", drawTime = "10:00 PM", modality = "CHANCE", number = "05", digits = 2, quantity = 25.0, unitPrice = 0.20, total = 5.00))),
                Triple("#000132", "Elena Supervisora", listOf(SaleItem(id = "item_demo_12", saleId = "sale_demo_09", drawId = "draw_nic_10pm", drawName = "Nica", drawIcon = "🇳🇮", drawTime = "10:00 PM", modality = "CHANCE", number = "77", digits = 2, quantity = 20.0, unitPrice = 0.20, total = 4.00)))
            )

            nicaExtraSales.forEachIndexed { idx, (ticketNo, client, items) ->
                val saleExtra = Sale(
                    id = "sale_demo_0${idx + 6}",
                    ticketNumber = ticketNo,
                    customerId = cust1?.id ?: "cust_01",
                    customerName = client,
                    userId = sellerId,
                    userName = sellerName,
                    subtotal = items.sumOf { it.total },
                    commission = items.sumOf { it.total } * 0.15,
                    total = items.sumOf { it.total },
                    status = "ACTIVA",
                    createdAt = System.currentTimeMillis() - (500000 * (idx + 1))
                )
                db.saleDao().insertSale(saleExtra)
                db.saleDao().insertSaleItems(items)
            }

            // Sales from Yesterday for Archive multi-day verification
            val yesterdayMillis = System.currentTimeMillis() - 86400000L
            val ySale1 = Sale(
                id = "sale_yesterday_01",
                ticketNumber = "#000135",
                customerId = cust1?.id ?: "cust_01",
                customerName = "Marcos Alvarado",
                userId = sellerId,
                userName = sellerName,
                subtotal = 26.47,
                commission = 3.97,
                total = 26.47,
                status = "ACTIVA",
                createdAt = yesterdayMillis - 3600000 * 3
            )
            val yItems1 = listOf(
                SaleItem(id = "item_y_01", saleId = ySale1.id, drawId = "draw_ang_9am", drawName = "Anguila", drawIcon = "🇦🇮", drawTime = "9:00 AM", modality = "CHANCE", number = "21", digits = 2, quantity = 5.0, unitPrice = 0.20, total = 1.00),
                SaleItem(id = "item_y_02", saleId = ySale1.id, drawId = "draw_ang_9am", drawName = "Anguila", drawIcon = "🇦🇮", drawTime = "9:00 AM", modality = "CHANCE", number = "88", digits = 2, quantity = 10.0, unitPrice = 0.20, total = 2.00),
                SaleItem(id = "item_y_03", saleId = ySale1.id, drawId = "draw_ang_9am", drawName = "Anguila", drawIcon = "🇦🇮", drawTime = "9:00 AM", modality = "CHANCE", number = "45", digits = 2, quantity = 50.0, unitPrice = 0.20, total = 10.00),
                SaleItem(id = "item_y_04", saleId = ySale1.id, drawId = "draw_ang_9am", drawName = "Anguila", drawIcon = "🇦🇮", drawTime = "9:00 AM", modality = "CHANCE", number = "99", digits = 2, quantity = 67.0, unitPrice = 0.20, total = 13.47)
            )
            db.saleDao().insertSale(ySale1)
            db.saleDao().insertSaleItems(yItems1)

            val ySale2 = Sale(
                id = "sale_yesterday_02",
                ticketNumber = "#000136",
                customerId = cust2?.id ?: "cust_02",
                customerName = "Rosa Moreno",
                userId = sellerId,
                userName = sellerName,
                subtotal = 38.50,
                commission = 5.77,
                total = 38.50,
                status = "ACTIVA",
                createdAt = yesterdayMillis - 3600000 * 2
            )
            val yItems2 = listOf(
                SaleItem(id = "item_y_05", saleId = ySale2.id, drawId = "draw_monazo_530pm", drawName = "Monazo", drawIcon = "🇨🇷", drawTime = "5:30 PM", modality = "CHANCE", number = "82", digits = 2, quantity = 80.0, unitPrice = 0.20, total = 16.00),
                SaleItem(id = "item_y_06", saleId = ySale2.id, drawId = "draw_monazo_530pm", drawName = "Monazo", drawIcon = "🇨🇷", drawTime = "5:30 PM", modality = "CHANCE", number = "14", digits = 2, quantity = 112.0, unitPrice = 0.20, total = 22.50)
            )
            db.saleDao().insertSale(ySale2)
            db.saleDao().insertSaleItems(yItems2)
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Seed Users
            val adminUser = User(
                id = "user_admin_01",
                name = "Admin Sistema",
                username = "admin",
                role = "ADMINISTRADOR",
                active = true
            )
            val sellerUser = User(
                id = "user_seller_01",
                name = "Carlos Vendedor",
                username = "vendedor",
                role = "VENDEDOR",
                active = true
            )
            val supervisorUser = User(
                id = "user_sup_01",
                name = "Elena Supervisora",
                username = "supervisor",
                role = "SUPERVISOR",
                active = true
            )
            db.userDao().insertUsers(listOf(adminUser, sellerUser, supervisorUser))

            // Seed Customers
            val cust1 = Customer(id = "cust_01", name = "Juan Pérez", alias = "Juancho", phone = "8899-1122")
            val cust2 = Customer(id = "cust_02", name = "María Rodríguez", alias = "Doña María", phone = "8765-4321")
            val cust3 = Customer(id = "cust_03", name = "Carlos Gómez", alias = "Carlitos", phone = "8432-1100")
            val cust4 = Customer(id = "cust_04", name = "Ana Morales", alias = "Anita", phone = "8900-5544")
            db.customerDao().insertCustomers(listOf(cust1, cust2, cust3, cust4))

            // Seed Initial Demo Sorteos (Custom administrator items, strictly ID referenced)
            val demoDraws = listOf(
                Draw(
                    id = "draw_prim_6pm",
                    name = "La Primera",
                    icon = "🇩🇴",
                    drawDate = "31/08/2026",
                    drawTime = "6:00 PM",
                    closingTime = "5:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_monazo_530pm",
                    name = "Monazo",
                    icon = "🇨🇷",
                    drawDate = "31/08/2026",
                    drawTime = "5:30 PM",
                    closingTime = "5:20 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ang_7pm",
                    name = "Anguila (HR PANAMA)",
                    icon = "🇦🇮",
                    drawDate = "31/08/2026",
                    drawTime = "7:00 PM",
                    closingTime = "6:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ang_9am",
                    name = "Anguila",
                    icon = "🇦🇮",
                    drawTime = "9:00 AM",
                    closingTime = "8:50 AM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ang_10am",
                    name = "Anguila",
                    icon = "🇦🇮",
                    drawTime = "10:00 AM",
                    closingTime = "9:50 AM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_prim_11am",
                    name = "La Primera",
                    icon = "🇩🇴",
                    drawTime = "11:00 AM",
                    closingTime = "10:50 AM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_hon_12pm",
                    name = "Honduras",
                    icon = "🇭🇳",
                    drawTime = "12:00 PM",
                    closingTime = "11:50 AM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_nic_12pm",
                    name = "Nica",
                    icon = "🇳🇮",
                    drawTime = "12:00 PM",
                    closingTime = "11:50 AM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_flo_1230pm",
                    name = "Florida",
                    icon = "🇺🇸",
                    drawTime = "12:30 PM",
                    closingTime = "12:20 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ang_1pm",
                    name = "Anguila",
                    icon = "🇦🇮",
                    drawTime = "1:00 PM",
                    closingTime = "12:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ny_130pm",
                    name = "New York",
                    icon = "🗽",
                    drawTime = "1:30 PM",
                    closingTime = "1:20 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_mon_155pm",
                    name = "Monazo",
                    icon = "🇨🇷",
                    drawTime = "1:55 PM",
                    closingTime = "1:45 PM",
                    allowedModalities = "CHANCE",
                    allowedDigits = "2,3",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ang_3pm",
                    name = "Anguila",
                    icon = "🇦🇮",
                    drawTime = "3:00 PM",
                    closingTime = "2:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_nic_4pm",
                    name = "Nica",
                    icon = "🇳🇮",
                    drawTime = "4:00 PM",
                    closingTime = "3:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_tica_830pm",
                    name = "Tica",
                    icon = "🇨🇷",
                    drawTime = "8:30 PM",
                    closingTime = "8:20 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_flo_850pm",
                    name = "Florida",
                    icon = "🌴",
                    drawTime = "8:50 PM",
                    closingTime = "8:40 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_ny_930pm",
                    name = "New York",
                    icon = "🗽",
                    drawTime = "9:30 PM",
                    closingTime = "9:20 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_hon_10pm",
                    name = "Honduras",
                    icon = "🇭🇳",
                    drawTime = "10:00 PM",
                    closingTime = "9:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                ),
                Draw(
                    id = "draw_nic_10pm",
                    name = "Nica",
                    icon = "🇳🇮",
                    drawTime = "10:00 PM",
                    closingTime = "9:50 PM",
                    allowedModalities = "CHANCE,PALE",
                    allowedDigits = "1,2,3,4",
                    recurrenceDays = "1,2,3,4,5,6,7",
                    active = true
                )
            )
            db.drawDao().insertDraws(demoDraws)

            // Seed Global & Specific Prices
            val initialPrices = mutableListOf<PriceConfig>()
            // Global fallback prices
            for (digits in 1..4) {
                initialPrices.add(
                    PriceConfig(
                        drawId = "GLOBAL",
                        modality = "CHANCE",
                        digits = digits,
                        unitPrice = 0.20
                    )
                )
            }
            for (digits in 2..4) {
                initialPrices.add(
                    PriceConfig(
                        drawId = "GLOBAL",
                        modality = "PALE",
                        digits = digits,
                        unitPrice = 0.20
                    )
                )
            }

            // Also seed specific prices for demo draws
            demoDraws.forEach { draw ->
                for (digits in 1..4) {
                    initialPrices.add(
                        PriceConfig(
                            drawId = draw.id,
                            modality = "CHANCE",
                            digits = digits,
                            unitPrice = if (draw.name == "Florida") 0.30 else 0.20
                        )
                    )
                }
                for (digits in 2..4) {
                    initialPrices.add(
                        PriceConfig(
                            drawId = draw.id,
                            modality = "PALE",
                            digits = digits,
                            unitPrice = if (draw.name == "Florida") 0.30 else 0.20
                        )
                    )
                }
            }
            db.priceDao().insertPrices(initialPrices)

            // Seed initial sample sales to illustrate tickets & dashboard
            val sale1 = Sale(
                id = "sale_demo_01",
                ticketNumber = "#000124",
                customerId = cust1.id,
                customerName = "Evelio",
                userId = sellerUser.id,
                userName = "Carlos Vendedor",
                subtotal = 0.20,
                commission = 0.03,
                total = 0.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 4
            )
            val items1 = listOf(
                SaleItem(
                    id = "item_demo_01",
                    saleId = sale1.id,
                    drawId = "draw_ang_9am",
                    drawName = "Anguila",
                    drawIcon = "🇦🇮",
                    drawTime = "9:00 AM",
                    modality = "CHANCE",
                    number = "21",
                    digits = 2,
                    quantity = 1.0,
                    unitPrice = 0.20,
                    total = 0.20
                )
            )
            db.saleDao().insertSale(sale1)
            db.saleDao().insertSaleItems(items1)

            val sale2 = Sale(
                id = "sale_demo_02",
                ticketNumber = "#000125",
                customerId = cust2.id,
                customerName = "María Rodríguez",
                userId = sellerUser.id,
                userName = "Carlos Vendedor",
                subtotal = 33.40,
                commission = 5.01,
                total = 33.40,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 3
            )
            val items2 = listOf(
                SaleItem(
                    id = "item_demo_03",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "44",
                    digits = 2,
                    quantity = 50.0,
                    unitPrice = 0.20,
                    total = 10.00
                ),
                SaleItem(
                    id = "item_demo_04",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "16",
                    digits = 2,
                    quantity = 60.0,
                    unitPrice = 0.20,
                    total = 12.00
                ),
                SaleItem(
                    id = "item_demo_05",
                    saleId = sale2.id,
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    drawIcon = "🇳🇮",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "69",
                    digits = 2,
                    quantity = 57.0,
                    unitPrice = 0.20,
                    total = 11.40
                )
            )
            db.saleDao().insertSale(sale2)
            db.saleDao().insertSaleItems(items2)

            // Sale 3: Honduras 10:00 PM (Premios $33.00 > Ventas $28.20) -> RED CARD!
            val sale3 = Sale(
                id = "sale_demo_03",
                ticketNumber = "#000126",
                customerId = cust1.id,
                customerName = "Roberto Gómez",
                userId = sellerUser.id,
                userName = "Carlos Vendedor",
                subtotal = 28.20,
                commission = 4.23,
                total = 28.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000 * 2
            )
            val items3 = listOf(
                SaleItem(
                    id = "item_demo_06",
                    saleId = sale3.id,
                    drawId = "draw_hon_10pm",
                    drawName = "Honduras",
                    drawIcon = "🇭🇳",
                    drawTime = "10:00 PM",
                    modality = "CHANCE",
                    number = "40", // Wins 1st prize
                    digits = 2,
                    quantity = 5.0,
                    unitPrice = 0.10, // 5 * 0.10 * 70 = $35.00 > 28.20
                    total = 28.20
                )
            )
            db.saleDao().insertSale(sale3)
            db.saleDao().insertSaleItems(items3)

            // Sale 4: New York 9:30 PM (Premios $22.00 > Ventas $15.80) -> RED CARD!
            val sale4 = Sale(
                id = "sale_demo_04",
                ticketNumber = "#000127",
                customerId = cust2.id,
                customerName = "Laura Silva",
                userId = sellerUser.id,
                userName = "Carlos Vendedor",
                subtotal = 15.80,
                commission = 2.37,
                total = 15.80,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 3600000
            )
            val items4 = listOf(
                SaleItem(
                    id = "item_demo_07",
                    saleId = sale4.id,
                    drawId = "draw_ny_930pm",
                    drawName = "New York",
                    drawIcon = "🗽",
                    drawTime = "9:30 PM",
                    modality = "CHANCE",
                    number = "42", // Wins 1st prize
                    digits = 2,
                    quantity = 3.0,
                    unitPrice = 0.10,
                    total = 15.80
                )
            )
            db.saleDao().insertSale(sale4)
            db.saleDao().insertSaleItems(items4)

            // Sale 5: Tica 8:30 PM (Premios $110.00 > Ventas $42.20) -> RED CARD!
            val sale5 = Sale(
                id = "sale_demo_05",
                ticketNumber = "#000128",
                customerId = cust1.id,
                customerName = "Fernando Castro",
                userId = sellerUser.id,
                userName = "Carlos Vendedor",
                subtotal = 42.20,
                commission = 6.33,
                total = 42.20,
                status = "ACTIVA",
                createdAt = System.currentTimeMillis() - 1800000
            )
            val items5 = listOf(
                SaleItem(
                    id = "item_demo_08",
                    saleId = sale5.id,
                    drawId = "draw_tica_830pm",
                    drawName = "Tica",
                    drawIcon = "🇨🇷",
                    drawTime = "8:30 PM",
                    modality = "CHANCE",
                    number = "35", // Wins 1st prize
                    digits = 2,
                    quantity = 8.0,
                    unitPrice = 0.20, // 8 * 0.20 * 70 = $112.00 > $42.20
                    total = 42.20
                )
            )
            db.saleDao().insertSale(sale5)
            db.saleDao().insertSaleItems(items5)

            // Seed Audit log
            val log1 = AuditLog(
                id = "audit_01",
                userId = adminUser.id,
                userName = adminUser.name,
                action = "CREAR",
                entity = "SORTEO",
                entityId = "draw_ang_9am",
                oldValue = null,
                newValue = "Sorteo Anguila 9:00 AM creado con éxito",
                createdAt = System.currentTimeMillis() - 86400000
            )
            db.auditDao().insertLog(log1)

            // Seed Initial Draw Results matching screenshots
            val demoResults = listOf(
                DrawResult(
                    id = "res_ang_9am",
                    drawId = "draw_ang_9am",
                    drawName = "Anguila",
                    firstPrize = "94",
                    secondPrize = "04",
                    thirdPrize = "03",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_nic_10pm",
                    drawId = "draw_nic_10pm",
                    drawName = "Nica",
                    firstPrize = "44",
                    secondPrize = "16",
                    thirdPrize = "69",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_hon_10pm",
                    drawId = "draw_hon_10pm",
                    drawName = "Honduras",
                    firstPrize = "40",
                    secondPrize = "91",
                    thirdPrize = "56",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_ny_930pm",
                    drawId = "draw_ny_930pm",
                    drawName = "New York",
                    firstPrize = "42",
                    secondPrize = "62",
                    thirdPrize = "39",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_flo_850pm",
                    drawId = "draw_flo_850pm",
                    drawName = "Florida",
                    firstPrize = "04",
                    secondPrize = "35",
                    thirdPrize = "43",
                    drawDate = "29/08/2026"
                ),
                DrawResult(
                    id = "res_tica_830pm",
                    drawId = "draw_tica_830pm",
                    drawName = "Tica",
                    firstPrize = "35",
                    secondPrize = "96",
                    thirdPrize = "60",
                    drawDate = "29/08/2026"
                )
            )
            db.drawResultDao().insertResults(demoResults)
        }
    }
}
