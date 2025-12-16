/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025-2030 The GoldenEraGlobal Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package global.goldenera.directory.api.v1.cryptoj;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.ethereum.Wei;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import global.goldenera.cryptoj.builder.TxBuilder;
import global.goldenera.cryptoj.builder.payloads.BipVoteBuilder;
import global.goldenera.cryptoj.common.Tx;
import global.goldenera.cryptoj.datatypes.Address;
import global.goldenera.cryptoj.datatypes.Hash;
import global.goldenera.cryptoj.datatypes.PrivateKey;
import global.goldenera.cryptoj.enums.BipVoteType;
import global.goldenera.cryptoj.enums.Network;
import global.goldenera.cryptoj.enums.TxType;
import global.goldenera.cryptoj.exceptions.CryptoJException;
import global.goldenera.cryptoj.serialization.tx.TxEncoder;
import global.goldenera.cryptoj.utils.Amounts;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJKeysDtoV1;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipAddressAliasAddDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipAddressAliasRemoveDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipAddressAuthorityAddRemoveDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipAddressValidatorAddRemoveDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipNetworkParamsSetDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipTokenBurnDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipTokenCreateDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipTokenMintDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipTokenUpdateDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInBipVoteDto;
import global.goldenera.directory.api.v1.cryptoj.dtos.CryptoJTxInTransfer;
import global.goldenera.directory.exceptions.GERuntimeException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/v1/cryptoj")
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CryptoJApiV1 {

        @GetMapping("generate-keys")
        public CryptoJKeysDtoV1 generateKeys() {
                String mnemonic = PrivateKey.generateMnemonic();
                try {
                        PrivateKey privateKey = PrivateKey.load(mnemonic, null);
                        return new CryptoJKeysDtoV1(mnemonic, privateKey.toHexString(),
                                        privateKey.getAddress().toChecksumAddress());
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/transfer")
        public CryptoJTxDto generateTxTransfer(@RequestBody CryptoJTxInTransfer input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei amount = Amounts.tokensWithDecimals(input.getAmount(),
                                input.getTokenDecimals() == null ? Amounts.STANDARD_DECIMALS
                                                : input.getTokenDecimals());
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Address tokenAddress = input.getTokenAddress() == null ? Address.NATIVE_TOKEN
                                : Address.fromHexString(input.getTokenAddress());
                Address recipientAddress = Address.fromHexString(input.getRecipientAddress());
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                try {
                        Tx transferTx = TxBuilder.create()
                                        .type(TxType.TRANSFER)
                                        .network(network)
                                        .recipient(recipientAddress)
                                        .tokenAddress(tokenAddress)
                                        .amount(amount)
                                        .fee(fee)
                                        .nonce(nonce)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(transferTx, true).toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        /*
         * 
         * BIP_VOTE(9);
         */

        @PostMapping("generate-tx/bip/address-alias/add")
        public CryptoJTxDto generateTxBipAddressAliasAdd(@RequestBody CryptoJTxInBipAddressAliasAddDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Address address = Address.fromHexString(input.getAddress());
                String alias = input.getAlias();

                try {
                        Tx addressAliasAddTx = TxBuilder.create()
                                        .addAddressAlias()
                                        .address(address)
                                        .alias(alias)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(addressAliasAddTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/address-alias/remove")
        public CryptoJTxDto generateTxBipAddressAliasRemove(@RequestBody CryptoJTxInBipAddressAliasRemoveDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));
                String alias = input.getAlias();

                try {
                        Tx addressAliasRemoveTx = TxBuilder.create()
                                        .removeAddressAlias()
                                        .alias(alias)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(addressAliasRemoveTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/authority/add")
        public CryptoJTxDto generateTxBipAuthorityAdd(@RequestBody CryptoJTxInBipAddressAuthorityAddRemoveDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));
                Address address = Address.fromHexString(input.getAddress());

                try {
                        Tx authorityAddTx = TxBuilder.create()
                                        .addAuthority()
                                        .authority(address)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(authorityAddTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/authority/remove")
        public CryptoJTxDto generateTxBipAuthorityRemove(
                        @RequestBody CryptoJTxInBipAddressAuthorityAddRemoveDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));
                Address address = Address.fromHexString(input.getAddress());

                try {
                        Tx authorityRemoveTx = TxBuilder.create()
                                        .removeAuthority()
                                        .authority(address)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(authorityRemoveTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/validator/add")
        public CryptoJTxDto generateTxBipValidatorAdd(@RequestBody CryptoJTxInBipAddressValidatorAddRemoveDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));
                Address address = Address.fromHexString(input.getAddress());

                try {
                        Tx validatorAddTx = TxBuilder.create()
                                        .addValidator()
                                        .validator(address)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(validatorAddTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/validator/remove")
        public CryptoJTxDto generateTxBipValidatorRemove(
                        @RequestBody CryptoJTxInBipAddressValidatorAddRemoveDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));
                Address address = Address.fromHexString(input.getAddress());

                try {
                        Tx validatorRemoveTx = TxBuilder.create()
                                        .removeValidator()
                                        .validator(address)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(validatorRemoveTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/network-params/set")
        public CryptoJTxDto generateTxBipNetworkParamsSet(@RequestBody CryptoJTxInBipNetworkParamsSetDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Wei blockReward = input.getBlockReward() == null ? null
                                : Amounts.tokensWithDecimals(input.getBlockReward(), Amounts.STANDARD_DECIMALS);
                Address blockRewardPoolAddress = input.getBlockRewardPoolAddress() == null ? null
                                : Address.fromHexString(input.getBlockRewardPoolAddress());
                Long targetMiningTimeMs = input.getTargetMiningTimeMs();
                Long asertHalfLifeBlocks = input.getAsertHalfLifeBlocks();
                BigInteger minDifficulty = input.getMinDifficulty() == null ? null
                                : new BigInteger(input.getMinDifficulty());
                Wei minTxBaseFee = input.getMinTxBaseFee() == null ? null
                                : Amounts.tokensWithDecimals(input.getMinTxBaseFee(), Amounts.STANDARD_DECIMALS);
                Wei minTxByteFee = input.getMinTxByteFee() == null ? null
                                : Amounts.tokensWithDecimals(input.getMinTxByteFee(), Amounts.STANDARD_DECIMALS);

                try {
                        Tx networkParamsSetTx = TxBuilder.create()
                                        .setNetworkParams()
                                        .blockReward(blockReward)
                                        .blockRewardPoolAddress(blockRewardPoolAddress)
                                        .targetMiningTime(targetMiningTimeMs)
                                        .asertHalfLife(asertHalfLifeBlocks)
                                        .minDifficulty(minDifficulty)
                                        .minTxBaseFee(minTxBaseFee)
                                        .minTxByteFee(minTxByteFee)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(networkParamsSetTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/token/create")
        public CryptoJTxDto generateTxBipTokenCreate(@RequestBody CryptoJTxInBipTokenCreateDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                String name = input.getName();
                String smallestUnitName = input.getSmallestUnitName();
                int numberOfDecimals = input.getNumberOfDecimals();
                String websiteUrl = input.getWebsiteUrl();
                String logoUrl = input.getLogoUrl();
                BigInteger maxSupply = input.getMaxSupply() == null ? null
                                : Amounts.tokensWithDecimals(input.getMaxSupply(), numberOfDecimals).toBigInteger();
                boolean userBurnable = input.isUserBurnable();

                try {
                        Tx tokenCreateTx = TxBuilder.create()
                                        .tokenCreate()
                                        .name(name)
                                        .symbol(smallestUnitName)
                                        .decimals(numberOfDecimals)
                                        .website(websiteUrl)
                                        .logo(logoUrl)
                                        .maxSupply(maxSupply)
                                        .userBurnable(userBurnable)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(tokenCreateTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/token/update")
        public CryptoJTxDto generateTxBipTokenUpdate(@RequestBody CryptoJTxInBipTokenUpdateDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Address address = Address.fromHexString(input.getTokenAddress());
                String name = input.getName();
                String smallestUnitName = input.getSmallestUnitName();
                String websiteUrl = input.getWebsiteUrl();
                String logoUrl = input.getLogoUrl();

                try {
                        Tx tokenUpdateTx = TxBuilder.create()
                                        .tokenUpdate()
                                        .token(address)
                                        .name(name)
                                        .symbol(smallestUnitName)
                                        .website(websiteUrl)
                                        .logo(logoUrl)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(tokenUpdateTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/token/mint")
        public CryptoJTxDto generateTxBipTokenMint(@RequestBody CryptoJTxInBipTokenMintDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Address tokenAddress = input.getTokenAddress() == null ? Address.NATIVE_TOKEN
                                : Address.fromHexString(input.getTokenAddress());
                Integer tokenNumberOfDecimals = input.getTokenNumberOfDecimals() == null ? Amounts.STANDARD_DECIMALS
                                : input.getTokenNumberOfDecimals();
                Address recipient = Address.fromHexString(input.getRecipient());
                Wei amount = Amounts.tokensWithDecimals(input.getAmount(), tokenNumberOfDecimals);

                try {
                        Tx tokenMintTx = TxBuilder.create()
                                        .tokenMint()
                                        .token(tokenAddress)
                                        .recipient(recipient)
                                        .amount(amount)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(tokenMintTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/token/burn")
        public CryptoJTxDto generateTxBipTokenBurn(@RequestBody CryptoJTxInBipTokenBurnDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Address tokenAddress = input.getTokenAddress() == null ? Address.NATIVE_TOKEN
                                : Address.fromHexString(input.getTokenAddress());
                Integer tokenNumberOfDecimals = input.getTokenNumberOfDecimals() == null ? Amounts.STANDARD_DECIMALS
                                : input.getTokenNumberOfDecimals();
                Address sender = Address.fromHexString(input.getSender());
                Wei amount = Amounts.tokensWithDecimals(input.getAmount(), tokenNumberOfDecimals);

                try {
                        Tx tokenBurnTx = TxBuilder.create()
                                        .tokenBurn()
                                        .token(tokenAddress)
                                        .from(sender)
                                        .amount(amount)
                                        .done()
                                        .network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(tokenBurnTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }

        @PostMapping("generate-tx/bip/vote")
        public CryptoJTxDto generateTxBipVote(@RequestBody CryptoJTxInBipVoteDto input) {
                Network network = input.getNetwork();
                PrivateKey privateKey = PrivateKey.wrap(Bytes.fromHexString(input.getPrivateKeyHex()));
                Wei fee = Amounts.tokensWithDecimals(input.getFee(), Amounts.STANDARD_DECIMALS);
                Long nonce = input.getNonce();
                Bytes message = input.getMessage() == null ? null
                                : Bytes.wrap(input.getMessage().getBytes(StandardCharsets.UTF_8));

                Hash referenceHash = Hash.fromHexString(input.getReferenceHash());
                BipVoteType bipVoteType = input.getBipVoteType();

                try {
                        BipVoteBuilder bipVoteTxBuilder = TxBuilder.create()
                                        .vote();

                        if (bipVoteType == BipVoteType.APPROVAL) {
                                bipVoteTxBuilder = bipVoteTxBuilder.approve(referenceHash);
                        } else {
                                bipVoteTxBuilder = bipVoteTxBuilder.disapprove(referenceHash);
                        }

                        Tx bipVoteTx = bipVoteTxBuilder.done().network(network)
                                        .nonce(nonce)
                                        .fee(fee)
                                        .message(message)
                                        .sign(privateKey);
                        return CryptoJTxDto.builder()
                                        .rawTxDataInHex(TxEncoder.INSTANCE.encode(bipVoteTx, true)
                                                        .toHexString())
                                        .build();
                } catch (CryptoJException e) {
                        throw new GERuntimeException(e.getMessage());
                }
        }
}
