package br.com.zup.edu.keymanager.registra

import br.com.zup.edu.KeyManagerGrpcServiceGrpc
import br.com.zup.edu.RegistraChavePixRequest
import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import br.com.zup.edu.keymanager.chavepix.client.bcb.*
import br.com.zup.edu.keymanager.chavepix.client.bcb.CreatePixKeyRequest.Companion.toBcb
import br.com.zup.edu.keymanager.chavepix.client.itau.InstituicaoClientResponse
import br.com.zup.edu.keymanager.chavepix.client.itau.ItauClientContaResponse
import br.com.zup.edu.keymanager.chavepix.client.itau.ItauErpClient
import br.com.zup.edu.keymanager.chavepix.client.itau.TitularClientResponse
import br.com.zup.edu.keymanager.chavepix.pix.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject


@MicronautTest(transactional = false)
internal class RegistraNovaChavePixEndPointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeyManagerGrpcServiceGrpc.KeyManagerGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ItauErpClient

    @Inject
    lateinit var bcbClient: BcbClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @MockBean(ItauErpClient::class)
    fun itauClient(): ItauErpClient? {
        return Mockito.mock(ItauErpClient::class.java)
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    private fun createPixKeyResponse(keyType: KeyType, key: String): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = keyType,
            key = key,
            bankAccount = BankAccountResponse(
                participant = "60701190",
                branch = "0001",
                accountNumber = "123456",
                accountType = AccountType.CACC
            ),
            owner = OwnerResponse(
                type = Type.NATURAL_PERSON,
                name = "Victor Barbosa",
                taxIdNumber = "04927154084"
            ), createdAt = LocalDateTime.now()
        )
    }

    private fun dadosDaContaResponse(): ItauClientContaResponse {
        return ItauClientContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoClientResponse("UNIBANCO ITAU SA", "60701190"),
            agencia = "0001",
            numero = "123456",
            titular = TitularClientResponse("Victor Barbosa", "04927154084")
        )
    }

    private fun novaChavePixEmail(): ChavePix {
        return ChavePix(
            tipoChave = br.com.zup.edu.keymanager.chavepix.pix.TipoChave.EMAIL,
            chave = "victor@zup.com",
            contaAssociada = ContaAssociada(
                tipoConta = TipoConta.CONTA_CORRENTE,
                instituicao = Instituicao(nomeInstituicao = "ITAU", ispb = "60701190"),
                agencia = "0001",
                numeroConta = "123456",
                titular = Titular(
                    titularId = CLIENTE_ID,
                    nomeTitular = "Victor Barbosa",
                    cpf = "04927154084"
                )
            )
        )
    }

    private fun novaChavePixCpf(): ChavePix {
        return ChavePix(
            tipoChave = br.com.zup.edu.keymanager.chavepix.pix.TipoChave.CPF,
            chave = "04927154084",
            contaAssociada = ContaAssociada(
                tipoConta = TipoConta.CONTA_CORRENTE,
                instituicao = Instituicao(nomeInstituicao = "ITAU", ispb = "60701190"),
                agencia = "0001",
                numeroConta = "123456",
                titular = Titular(
                    titularId = CLIENTE_ID,
                    nomeTitular = "Victor Barbosa",
                    cpf = "04927154084"
                )
            )
        )
    }

    private fun novaChavePixCelular(): ChavePix {
        return ChavePix(
            tipoChave = br.com.zup.edu.keymanager.chavepix.pix.TipoChave.CELULAR,
            chave = "+5549968094131",
            contaAssociada = ContaAssociada(
                tipoConta = TipoConta.CONTA_CORRENTE,
                instituicao = Instituicao(nomeInstituicao = "ITAU", ispb = "60701190"),
                agencia = "0001",
                numeroConta = "123456",
                titular = Titular(
                    titularId = CLIENTE_ID,
                    nomeTitular = "Victor Barbosa",
                    cpf = "04927154084"
                )
            )
        )
    }

    private fun novaChavePixAleatoria(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = KeyType.RANDOM,
            key = "",
            bankAccount = BankAccount(
                participant = "60701190",
                branch = "0001",
                accountNumber = "123456",
                accountType = AccountType.CACC
            ), owner = Owner(type = Type.NATURAL_PERSON, name = "Victor Barbosa", taxIdNumber = "04927154084")
        )
    }

    @Test
    fun `DEVE registrar chave pix CPF`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixCpf().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.CPF, key = "04927154084")))

        grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        ).let { assertNotNull(it.pixId) }

        val chaveRegistrada = repository.findByContaAssociadaTitularCpf(dadosDaContaResponse().titular.cpf).get()

        assertNotNull(chaveRegistrada.id)
        assertEquals(dadosDaContaResponse().titular.cpf, chaveRegistrada.contaAssociada.titular.cpf)
        assertTrue(chaveRegistrada.tipoChave == br.com.zup.edu.keymanager.chavepix.pix.TipoChave.CPF)
    }


    @Test
    fun `DEVE registrar chave pix EMAIL`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixEmail().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.EMAIL, key = "victor@zup.com")))

        grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.EMAIL)
                .setChave("victor@zup.com")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        ).let { assertNotNull(it.pixId) }

        val chaveRegistrada = repository.findByChave("victor@zup.com").get()

        assertNotNull(chaveRegistrada.id)
        assertEquals("victor@zup.com", chaveRegistrada.chave)
        assertTrue(chaveRegistrada.tipoChave == br.com.zup.edu.keymanager.chavepix.pix.TipoChave.EMAIL)

    }


    @Test
    fun `DEVE registrar chave pix CELULAR`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixCelular().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.PHONE, key = "+5549968094131")))

        grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CELULAR)
                .setChave("+5549968094131")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        ).let { assertNotNull(it.pixId) }

        val chaveRegistrada = repository.findByChave("+5549968094131").get()

        assertNotNull(chaveRegistrada.id)
        assertEquals("+5549968094131", chaveRegistrada.chave)
        assertTrue(chaveRegistrada.tipoChave == br.com.zup.edu.keymanager.chavepix.pix.TipoChave.CELULAR)

    }


    @Test
    fun `DEVE registrar chave pix ALEATORIA`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixAleatoria()))
            .thenReturn(
                HttpResponse.ok(
                    createPixKeyResponse(
                        keyType = KeyType.RANDOM,
                        key = UUID.randomUUID().toString()
                    )
                )
            )

        grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.ALEATORIA)
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        ).let { assertNotNull(it.pixId) }

        val chaveRegistrada = repository.findByContaAssociadaTitularTitularId(CLIENTE_ID).get()

        assertNotNull(chaveRegistrada.id)
        assertNotNull(chaveRegistrada.chave)
        assertTrue(chaveRegistrada.tipoChave == br.com.zup.edu.keymanager.chavepix.pix.TipoChave.ALEATORIA)
    }


    @Test
    fun `NAO deve registrar chave quando ja existe uma igual`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixEmail().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.EMAIL, key = "victor@zup.com")))

        repository.save(novaChavePixEmail())

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.EMAIL)
                    .setChave("victor@zup.com")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.ALREADY_EXISTS.code, it.status.code)
            assertEquals(
                "Chave ${br.com.zup.edu.keymanager.chavepix.pix.TipoChave.EMAIL}: victor@zup.com já cadastrada",
                it.status.description
            )
        }
    }


    @Test
    fun `NAO deve registrar chave pix com parametros invalidos`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixEmail().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.EMAIL, key = "victor@zup.com")))

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder().build())
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Dados inválidos", it.status.description)
        }

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId("CLIENTE_ID.toString()")
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Dados inválidos", it.status.description)
        }

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Dados inválidos", it.status.description)
        }

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Dados inválidos", it.status.description)
        }

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .setTipoChave(TipoChave.UNKNOWN_CHAVE)
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Dados inválidos", it.status.description)
        }

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoConta(TipoConta.UNKNOWN_CONTA)
                    .setTipoChave(TipoChave.ALEATORIA)
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Tipo de conta inválido", it.status.description)
        }
    }


    @Test
    fun `NAO deve registrar nova chave CPF para chave CPF ja cadastrado`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixCpf().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.CPF, key = "04927154084")))


        repository.save(novaChavePixCpf())

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.ALREADY_EXISTS.code, it.status.code)
            assertEquals(
                "Chave CPF já cadastrada",
                it.status.description
            )
        }

    }


    @Test
    fun `NAO deve registrar chave pix quando Client ERPItau devolver algum erro de conexao ou cliente nao encontrado`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        `when`(bcbClient.cadastraChave(novaChavePixCpf().toBcb()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse(keyType = KeyType.CPF, key = "04927154084")))

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.INVALID_ARGUMENT.code, it.status.code)
            assertEquals("Id do cliente não encontrado no ItauErp", it.status.description)
        }
    }


    @Test
    fun `NAO deve registrar chave pix quando Client BCB devolver erro de cliente ja existente`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixCpf().toBcb()))
            .thenThrow(HttpClientResponseException("error", HttpResponse.unprocessableEntity<Any>()))

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.ALREADY_EXISTS.code, it.status.code)
            assertEquals("Chave pix já cadastrada no BCB", it.status.description)
        }


    }


    @Test
    fun `NAO deve registrar chave pix quando client BCB estiver fora do ar`() {
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipoConta = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(novaChavePixCpf().toBcb()))
            .thenThrow(HttpClientException("error"))

        assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }.let {
            assertEquals(Status.FAILED_PRECONDITION.code, it.status.code)
            assertEquals("Não foi possível conectar ao sistema BCB, tente mais tarde", it.status.description)
        }
    }
}


