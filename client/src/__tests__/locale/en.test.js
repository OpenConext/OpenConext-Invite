import en from "../../locale/en";
import nl from "../../locale/nl";
import pt from "../../locale/pt";

expect.extend({
    toContainKey(translation, key) {
        return {
            message: () => `Expected ${key} to be present in ${JSON.stringify(translation)}`,
            pass: (translation !== undefined && translation[key] !== undefined)
        };
    },
});

test("All translations exists in all bundles", () => {
    const contains = (translation, translationToVerify, keyCollection, parents) => {
        Object.keys(translation).forEach(key => {
            expect(translationToVerify).toContainKey(key);
            const value = translation[key];
            keyCollection.push(parents + key);
            if (typeof value === "object") {
                contains(value, translationToVerify[key], keyCollection, parents + key + ".")
            }
        });
    };
    const keyCollectionEN = [];
    contains(en, nl, pt, keyCollectionEN, '');
    const keyCollectionNL = [];
    contains(nl, en, pt, keyCollectionNL, '');
    const keyCollectionPT = [];
    contains(pt, en, nl, keyCollectionPT, '');
    
    const positionalMismatchesEN_NL = keyCollectionEN.filter((item, index) => keyCollectionNL[index] !== item);
    const positionalMismatchesEN_PT = keyCollectionEN.filter((item, index) => keyCollectionPT[index] !== item);
    const positionalMismatchesNL_PT = keyCollectionNL.filter((item, index) => keyCollectionPT[index] !== item);
    
    expect(positionalMismatchesEN_NL).toEqual([]);
    expect(positionalMismatchesEN_PT).toEqual([]);
    expect(positionalMismatchesNL_PT).toEqual([]);
});
