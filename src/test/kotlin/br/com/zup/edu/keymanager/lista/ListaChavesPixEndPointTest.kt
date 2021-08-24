package br.com.zup.edu.keymanager.lista

import br.com.zup.edu.ListaChavePixRequest
import br.com.zup.edu.ListaChavePixServiceGrpc
import br.com.zup.edu.TipoConta
import br.com.zup.edu.keymanager.chavepix.pix.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChavesPixEndPointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: ListaChavePixServiceGrpc.ListaChavePixServiceBlockingStub
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
        val CHAVE = UUID.randomUUID()
    }

    private fun criaChavePix(tipoChave: TipoChave, chave: String, titularId: UUID): ChavePix {
        return ChavePix(
            tipoChave = tipoChave,
            chave = chave,
            ContaAssociada(
                tipoConta = TipoConta.CONTA_CORRENTE,
                instituicao = Instituicao(
                    nomeInstituicao = "ITAU UNIBANCO SA",
                    ispb = "60701190"
                ),
                agencia = "0001",
                numeroConta = "123456",
                titular = Titular(
                    titularId = titularId,
                    nomeTitular = "VICTOR BARBOSA",
                    cpf = "04927154084"
                )
            )
        )
    }

    @BeforeEach
    internal fun setUp() {
        repository.save(criaChavePix(TipoChave.CPF, "04927154084", CLIENTE_ID))
        repository.save(criaChavePix(TipoChave.EMAIL, "victor@zup.com", CLIENTE_ID))
        repository.save(criaChavePix(TipoChave.CELULAR, "+5549968094131", CLIENTE_ID))
        repository.save(criaChavePix(TipoChave.ALEATORIA, CHAVE.toString(), CLIENTE_ID))
        repository.save(criaChavePix(TipoChave.ALEATORIA, UUID.randomUUID().toString(), UUID.randomUUID()))
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `DEVE retornar lista de chaves de um usuario`() {
        grpcClient.lista(
            ListaChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .build()
        ).let {
            assertNotNull(it)
            assertEquals(4, it.chavesCount)
            assertEquals(CLIENTE_ID.toString(), it.clienteId)
        }

    }

    @Test
    fun `NAO deve retornar lista de chaves para ID inexistente ou sem chaves cadastradas`() {
        assertThrows<StatusRuntimeException> {
            grpcClient.lista(
                ListaChavePixRequest.newBuilder()
                    .setClienteId(UUID.randomUUID().toString())
                    .build()
            ).let { assertEquals(0, it.chavesCount) }
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Cliente não encontrado", it.status.description)
        }
    }
}