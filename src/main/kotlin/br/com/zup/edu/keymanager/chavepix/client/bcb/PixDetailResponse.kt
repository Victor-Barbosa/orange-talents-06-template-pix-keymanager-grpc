package br.com.zup.edu.keymanager.chavepix.client.bcb

import br.com.zup.edu.keymanager.chavepix.carrega.ChavePixInfo
import br.com.zup.edu.keymanager.chavepix.carrega.ContaAssociadaInfo
import br.com.zup.edu.keymanager.chavepix.carrega.TitularInfo
import br.com.zup.edu.keymanager.chavepix.pix.*
import java.time.LocalDateTime

data class PixDetailResponse(
    val keyType: KeyType,
    val key: String,
    val bankAccount: BankAccountResponse,
    val owner: OwnerResponse,
    val createdAt: LocalDateTime
) {
    fun toInfo(): ChavePixInfo {
        return ChavePixInfo(
            tipoChave = KeyType.toTipoChave(keyType),
            chave = key,
            tipoConta = AccountType.toTipoConta(bankAccount.accountType),
            contaInfo = ContaAssociadaInfo(
                instituicao = Instituicao(
                    nomeInstituicao = Instituicoes.nome(bankAccount.participant),
                    ispb = bankAccount.participant
                ),
                agencia = bankAccount.branch,
                numeroConta = bankAccount.accountNumber,
                titularInfo = TitularInfo(
                    nomeTitular = owner.name,
                    cpf = owner.taxIdNumber
                )
            )
        )
    }

    fun toModel(): ChavePix {
        return ChavePix(
            tipoChave = KeyType.toTipoChave(keyType),
            chave = key,
            contaAssociada = ContaAssociada(
                tipoConta = AccountType.toTipoConta(bankAccount.accountType),
                instituicao = Instituicao(
                    nomeInstituicao = Instituicoes.nome(bankAccount.participant),
                    ispb = bankAccount.participant
                ),
                agencia = bankAccount.branch,
                numeroConta = bankAccount.accountNumber,
                titular = Titular(
                    titularId = null,
                    nomeTitular = owner.name,
                    cpf = owner.taxIdNumber
                )
            )
        )
    }

} 



