package br.com.zup.edu.keymanager.chavepix.registra

import br.com.zup.edu.TipoConta
import br.com.zup.edu.keymanager.chavepix.pix.ChavePix
import br.com.zup.edu.keymanager.chavepix.pix.ChavePixRepository
import br.com.zup.edu.keymanager.chavepix.pix.TipoChave
import br.com.zup.edu.keymanager.chavepix.client.bcb.BcbClient
import br.com.zup.edu.keymanager.chavepix.client.bcb.CreatePixKeyRequest.Companion.toBcb
import br.com.zup.edu.keymanager.chavepix.client.itau.ItauErpClient
import br.com.zup.edu.keymanager.chavepix.compartilhado.exceptions.ChavePixExistenteException
import br.com.zup.edu.keymanager.chavepix.compartilhado.exceptions.TipoChaveInvalidoException
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.validation.Validated
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid


@Validated
@Singleton
class NovaChavePixService(
    @Inject private val repository: ChavePixRepository,
    @Inject private val itauErpClient: ItauErpClient,
    @Inject private val bcbClient: BcbClient
) {

    @Transactional
    fun registra(@Valid novaChaveRequest: NovaChavePixRequest): ChavePix {

        if (novaChaveRequest.tipoConta == TipoConta.UNKNOWN_CONTA) {
            throw TipoChaveInvalidoException("Tipo de conta inválido")
        }

        if (repository.existsByChave(novaChaveRequest.chave)) {
            throw ChavePixExistenteException("Chave ${novaChaveRequest.tipoChave}: ${novaChaveRequest.chave} já cadastrada")
        }

        val chavePix: ChavePix
        try {
            val clientResponse =
                itauErpClient.buscaContaPorTipo(novaChaveRequest.clienteId, novaChaveRequest.tipoConta.name)
            if(clientResponse.status != HttpStatus.OK){
                throw IllegalArgumentException("Id do cliente não encontrado no ItauErp")
            }
            val contaAssociada = clientResponse.body()!!.toModel(novaChaveRequest.tipoConta, novaChaveRequest.clienteId)
            chavePix = novaChaveRequest.toModel(contaAssociada)
        } catch (e: HttpClientResponseException) {
            throw IllegalArgumentException("Id do cliente não encontrado")
        } catch (e: HttpClientException) {
            throw IllegalStateException("Não foi possível conectar ao sistema ItauErp, tente mais tarde")
        }

        if (chavePix.tipoChave == TipoChave.CPF && repository.existsByChave(chavePix.contaAssociada.titular.cpf)) {
            throw ChavePixExistenteException("Chave CPF já cadastrada")
        }

        repository.save(chavePix)
        val bcbRequest = chavePix.toBcb()
        try {
            val bcbResponse = bcbClient.cadastraChave(bcbRequest)
            if (bcbResponse.status == HttpStatus.CREATED) {
                chavePix.atualizaChave(bcbResponse.body())
            }
        } catch (e: HttpClientResponseException) {
            if (e.status == HttpStatus.UNPROCESSABLE_ENTITY) throw ChavePixExistenteException("Chave pix já cadastrada no BCB")

        } catch (e: HttpClientException) {
            throw IllegalStateException("Não foi possível conectar ao sistema BCB, tente mais tarde")
        }

        return chavePix
    }
}
